import lodash from 'lodash';
import 'jest-extended';
import { execYtt } from '../src/ytt';
import {
  parseYamlDocument,
  findConfigMap,
  findDeployment,
  deploymentContainer,
  containerEnvValues,
  containerEnvValue,
  findService
} from '../src/k8s-helper';
import { SCDF_SERVER_NAME, SKIPPER_NAME, DEFAULT_REQUIRED_DATA_VALUES } from '../src/constants';

describe('servers', () => {
  it('should have default service types', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const dataflowService = findService(yaml, SCDF_SERVER_NAME);
    expect(dataflowService).toBeTruthy();
    expect(dataflowService?.spec?.type).toBe('ClusterIP');

    const skipperService = findService(yaml, SKIPPER_NAME);
    expect(skipperService).toBeTruthy();
    expect(skipperService?.spec?.type).toBe('ClusterIP');
  });

  it('should have load balancer as service type', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.server.service.type=LoadBalancer',
        'scdf.skipper.service.type=LoadBalancer'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const dataflowService = findService(yaml, SCDF_SERVER_NAME);
    expect(dataflowService).toBeTruthy();
    expect(dataflowService?.spec?.type).toBe('LoadBalancer');

    const skipperService = findService(yaml, SKIPPER_NAME);
    expect(skipperService).toBeTruthy();
    expect(skipperService?.spec?.type).toBe('LoadBalancer');
  });

  it('should have tagged images', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    expect(dataflowContainer?.image).toBe('springcloud/spring-cloud-dataflow-server:2.8.1');

    const env = containerEnvValue(dataflowContainer, 'SPRING_CLOUD_DATAFLOW_TASK_COMPOSEDTASKRUNNER_URI');
    expect(env).toBe('docker://springcloud/spring-cloud-dataflow-composed-task-runner:2.8.1');

    const skipperDeployment = findDeployment(yaml, SKIPPER_NAME);
    const skipperContainer = deploymentContainer(skipperDeployment, SKIPPER_NAME);
    expect(skipperContainer?.image).toBe('springcloud/spring-cloud-skipper-server:2.7.1');
  });

  it('should have digested images', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        'scdf.server.image.digest=fakedigest1',
        'scdf.skipper.image.digest=fakedigest2',
        'scdf.ctr.image.digest=fakedigest3'
      ]
    });

    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    expect(dataflowContainer?.image).toBe('springcloud/spring-cloud-dataflow-server@fakedigest1');

    const env = containerEnvValue(dataflowContainer, 'SPRING_CLOUD_DATAFLOW_TASK_COMPOSEDTASKRUNNER_URI');
    expect(env).toBe('docker://springcloud/spring-cloud-dataflow-composed-task-runner@fakedigest3');

    const skipperDeployment = findDeployment(yaml, SKIPPER_NAME);
    const skipperContainer = deploymentContainer(skipperDeployment, SKIPPER_NAME);
    expect(skipperContainer?.image).toBe('springcloud/spring-cloud-skipper-server@fakedigest2');
  });

  it('no additional dataflow server config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const skipperConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const applicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : undefined;
    expect(applicationYaml).toContain('spring');
  });

  it('should have additional server config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.skipper.config.foo=bar'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const skipperConfigMap = findConfigMap(yaml, SKIPPER_NAME);

    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const dataflowFoo = lodash.get(dataflowJson, 'foo') as string;
    expect(dataflowFoo).toEqual('bar');

    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();
    const skipperFoo = lodash.get(skipperJson, 'foo') as string;
    expect(skipperFoo).toEqual('bar');
  });

  it('should have ctr image set', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.ctr.image.repository=fakerepo',
        'scdf.ctr.image.tag=faketag'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const container = deploymentContainer(deployment, SCDF_SERVER_NAME);
    const env = containerEnvValue(container, 'SPRING_CLOUD_DATAFLOW_TASK_COMPOSEDTASKRUNNER_URI');
    expect(env).toBe('docker://fakerepo:faketag');
  });

  it('skipper should have default env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SKIPPER_NAME);
    const container = deploymentContainer(deployment, SKIPPER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toHaveLength(4);
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: 'SPRING_CLOUD_CONFIG_ENABLED',
          value: 'false'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_KUBERNETES_CONFIG_ENABLE_API',
          value: 'false'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_KUBERNETES_SECRETS_ENABLE_API',
          value: 'false'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_KUBERNETES_SECRETS_PATHS',
          value: '/etc/secrets'
        })
      ])
    );
  });

  it('skipper should have monitoring env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.feature.monitoring.grafana.enabled=true'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SKIPPER_NAME);
    const container = deploymentContainer(deployment, SKIPPER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED',
          value: 'true'
        }),
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_ENABLED',
          value: 'true'
        }),
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_HOST',
          value: 'prometheus-rsocket-proxy'
        }),
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_PORT',
          value: '7001'
        })
      ])
    );
  });

  it('dataflow should have default env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const container = deploymentContainer(deployment, SCDF_SERVER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toHaveLength(11);
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: 'KUBERNETES_NAMESPACE',
          valueFrom: {
            fieldRef: {
              fieldPath: 'metadata.namespace'
            }
          }
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_CONFIG_ENABLED',
          value: 'false'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_DATAFLOW_FEATURES_ANALYTICS_ENABLED',
          value: 'true'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_DATAFLOW_FEATURES_SCHEDULES_ENABLED',
          value: 'true'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_DATAFLOW_TASK_COMPOSEDTASKRUNNER_URI',
          value: 'docker://springcloud/spring-cloud-dataflow-composed-task-runner:2.8.1'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_KUBERNETES_CONFIG_ENABLE_API',
          value: 'false'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_KUBERNETES_SECRETS_ENABLE_API',
          value: 'false'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_KUBERNETES_SECRETS_PATHS',
          value: '/etc/secrets'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_DATAFLOW_SERVER_URI'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI'
        }),
        expect.objectContaining({
          name: 'SPRING_APPLICATION_JSON'
        })
      ])
    );
  });

  it('dataflow should have monitoring env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.feature.monitoring.grafana.enabled=true'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const container = deploymentContainer(deployment, SCDF_SERVER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED',
          value: 'true'
        }),
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_ENABLED',
          value: 'true'
        }),
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_HOST',
          value: 'prometheus-rsocket-proxy'
        }),
        expect.objectContaining({
          name: 'MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_PORT',
          value: '7001'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_DATAFLOW_METRICS_DASHBOARD_URL'
        })
      ])
    );
  });

  it('monitoring dataflow server config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.grafana.image.tag=1.2.3',
        'scdf.feature.monitoring.prometheus.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const skipperConfigMap = findConfigMap(yaml, SKIPPER_NAME);
    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const enabled1 = lodash.get(dataflowJson, 'management.metrics.export.prometheus.enabled') as boolean;
    expect(enabled1).toBeTrue();
    const url = lodash.get(dataflowJson, 'spring.cloud.dataflow.metrics.dashboard.url') as string;
    expect(url).toBeFalsy();

    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();
    const enabled2 = lodash.get(skipperJson, 'management.metrics.export.prometheus.enabled') as boolean;
    expect(enabled2).toBeTrue();
  });

  it('dashboard metrics url', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.server.metrics.dashboard.url=http://fakedashboard'
      ]
    });
    expect(result.success).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const url = lodash.get(dataflowJson, 'spring.cloud.dataflow.metrics.dashboard.url') as string;
    expect(url).toEqual('http://fakedashboard');
  });
});
