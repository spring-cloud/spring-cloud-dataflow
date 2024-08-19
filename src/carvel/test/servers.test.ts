// @ts-ignore
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
    expect(result.success, result.stderr).toBeTruthy();
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
    expect(result.success, result.stderr).toBeTruthy();
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
    expect(result.success, result.stderr).toBeTruthy();
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

  it('should have default server config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const skipperConfigMap = findConfigMap(yaml, SKIPPER_NAME);

    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();

    const dataflowPlatformLimitsMemory = lodash.get(
      dataflowJson,
      'spring.cloud.dataflow.task.platform.kubernetes.accounts.default.limits.memory'
    ) as string;
    expect(dataflowPlatformLimitsMemory).toEqual('1024Mi');

    const dataflowPlatformImagePullSecret = lodash.get(
      dataflowJson,
      'spring.cloud.dataflow.task.platform.kubernetes.accounts.default.imagePullSecret'
    ) as string;
    expect(dataflowPlatformImagePullSecret).toEqual('reg-creds');

    const skipperPlatformLimitsMemory = lodash.get(
      skipperJson,
      'spring.cloud.skipper.server.platform.kubernetes.accounts.default.limits.memory'
    ) as string;
    expect(skipperPlatformLimitsMemory).toEqual('1024Mi');

    const skipperPlatformImagePullSecret = lodash.get(
      skipperJson,
      'spring.cloud.skipper.server.platform.kubernetes.accounts.default.imagePullSecret'
    ) as string;
    expect(skipperPlatformImagePullSecret).toEqual('reg-creds');
  });

  it('should change server config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES, 'scdf.registry.secret.ref=fakeref']
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const skipperConfigMap = findConfigMap(yaml, SKIPPER_NAME);

    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();

    const dataflowPlatformImagePullSecret = lodash.get(
      dataflowJson,
      'spring.cloud.dataflow.task.platform.kubernetes.accounts.default.imagePullSecret'
    ) as string;
    expect(dataflowPlatformImagePullSecret).toEqual('fakeref');

    const skipperPlatformImagePullSecret = lodash.get(
      skipperJson,
      'spring.cloud.skipper.server.platform.kubernetes.accounts.default.imagePullSecret'
    ) as string;
    expect(skipperPlatformImagePullSecret).toEqual('fakeref');
  });

  it('should have additional server config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.skipper.config.foo=bar'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
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
    expect(result.success, result.stderr).toBeTruthy();
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
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SKIPPER_NAME);
    const container = deploymentContainer(deployment, SKIPPER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toHaveLength(6);
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
          value: '/workspace/runtime/secrets'
        })
      ])
    );
  });

  it('skipper should have monitoring env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SKIPPER_NAME);
    const container = deploymentContainer(deployment, SKIPPER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: 'MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED',
          value: 'true'
        }),
        expect.objectContaining({
          name: 'MANAGEMENT_PROMETHEUS_METRICS_EXPORT_RSOCKET_ENABLED',
          value: 'true'
        })
      ])
    );
  });

  it('dataflow should have default env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const container = deploymentContainer(deployment, SCDF_SERVER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toHaveLength(12);
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
          value: '/workspace/runtime/secrets'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_DATAFLOW_SERVER_URI'
        }),
        expect.objectContaining({
          name: 'SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI'
        })
      ])
    );
  });

  it('dataflow should have extra env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.server.env=[{"name":"JAVA_TOOL_OPTIONS","value":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005"}]'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const container = deploymentContainer(deployment, SCDF_SERVER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: 'JAVA_TOOL_OPTIONS',
          value: '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005'
        })
      ])
    );
  });

  it('skipper should have extra env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.skipper.env=[{"name":"JAVA_TOOL_OPTIONS","value":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5006"}]'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SKIPPER_NAME);
    const container = deploymentContainer(deployment, SKIPPER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: 'JAVA_TOOL_OPTIONS',
          value: '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5006'
        })
      ])
    );
  });

  it('dataflow should have monitoring env values', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const deployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const container = deploymentContainer(deployment, SCDF_SERVER_NAME);
    const envs = containerEnvValues(container);
    expect(envs).toBeTruthy();
    expect(envs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: 'MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED', value: 'true' }),
        expect.objectContaining({ name: 'MANAGEMENT_PROMETHEUS_METRICS_EXPORT_RSOCKET_ENABLED', value: 'true' })
      ])
    );
  });

  it('monitoring dataflow server config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=localhost',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=localhost',
        'scdf.binder.kafka.zk.port=1235',
        'scdf.server.config.foo=bar',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const skipperConfigMap = findConfigMap(yaml, SKIPPER_NAME);
    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const enabled1 = lodash.get(dataflowJson, 'management.prometheus.metrics.export.enabled') as boolean;
    expect(enabled1).toBeTrue();
    const url = lodash.get(dataflowJson, 'spring.cloud.dataflow.metrics.dashboard.url') as string;
    expect(url).toBeFalsy();

    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();
    const enabled2 = lodash.get(skipperJson, 'management.prometheus.metrics.export.enabled') as boolean;
    expect(enabled2).toBeTrue();
  });

  it('dashboard metrics url', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true',
        'scdf.server.metrics.dashboard.url=http://fakedashboard'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const url = lodash.get(dataflowJson, 'spring.cloud.dataflow.metrics.dashboard.url') as string;
    expect(url).toEqual('http://fakedashboard');
  });

  it('should have default servlet context path', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const url = lodash.get(dataflowJson, 'server.servlet.context-path') as string;
    expect(url).toBeUndefined();

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    expect(dataflowContainer?.livenessProbe?.httpGet?.path).toBe('/management/health');
    expect(dataflowContainer?.readinessProbe?.httpGet?.path).toBe('/management/info');
  });

  it('should change server servlet context path', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES, 'scdf.server.contextPath=/scdf']
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const url = lodash.get(dataflowJson, 'server.servlet.context-path') as string;
    expect(url).toBe('/scdf');

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    expect(dataflowContainer?.livenessProbe?.httpGet?.path).toBe('/scdf/management/health');
    expect(dataflowContainer?.readinessProbe?.httpGet?.path).toBe('/scdf/management/info');
  });

  it('should have default resources', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const skipperDeployment = findDeployment(yaml, SKIPPER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    const skipperContainer = deploymentContainer(skipperDeployment, SKIPPER_NAME);

    expect(dataflowContainer?.resources?.requests?.cpu).toBe('500m');
    expect(dataflowContainer?.resources?.requests?.memory).toBe('1024Mi');

    expect(skipperContainer?.resources?.requests?.cpu).toBe('500m');
    expect(skipperContainer?.resources?.requests?.memory).toBe('1024Mi');
  });

  it('should change resources', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.server.resources.limits.cpu=600m',
        'scdf.server.resources.limits.memory=1000Mi',
        'scdf.server.resources.requests.cpu=600m',
        'scdf.server.resources.requests.memory=1000Mi',
        'scdf.skipper.resources.limits.cpu=600m',
        'scdf.skipper.resources.limits.memory=1000Mi',
        'scdf.skipper.resources.requests.cpu=600m',
        'scdf.skipper.resources.requests.memory=1000Mi'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const skipperDeployment = findDeployment(yaml, SKIPPER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    const skipperContainer = deploymentContainer(skipperDeployment, SKIPPER_NAME);

    expect(dataflowContainer?.resources?.limits?.cpu).toBe('600m');
    expect(dataflowContainer?.resources?.limits?.memory).toBe('1000Mi');
    expect(dataflowContainer?.resources?.requests?.cpu).toBe('600m');
    expect(dataflowContainer?.resources?.requests?.memory).toBe('1000Mi');

    expect(skipperContainer?.resources?.limits?.cpu).toBe('600m');
    expect(skipperContainer?.resources?.limits?.memory).toBe('1000Mi');
    expect(skipperContainer?.resources?.requests?.cpu).toBe('600m');
    expect(skipperContainer?.resources?.requests?.memory).toBe('1000Mi');
  });
});
