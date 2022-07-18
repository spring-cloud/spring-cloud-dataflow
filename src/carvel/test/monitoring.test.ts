import { execYtt } from '../src/ytt';
import { findDeployment, findService, deploymentContainer } from '../src/k8s-helper';
import {
  GRAFANA_NAME,
  PROMETHEUS_NAME,
  PROMETHEUS_RSOCKET_PROXY_NAME,
  DEFAULT_REQUIRED_DATA_VALUES
} from '../src/constants';

describe('monitoring', () => {
  it('should have grafana', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.grafana.image.tag=1.2.3'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const grafanaDeployment = findDeployment(yaml, GRAFANA_NAME);
    expect(grafanaDeployment).toBeTruthy();
    const grafanaContainer = deploymentContainer(grafanaDeployment, GRAFANA_NAME);
    expect(grafanaContainer?.image).toContain('springcloud/spring-cloud-dataflow-grafana-prometheus:1.2.3');

    const grafanaService = findService(yaml, GRAFANA_NAME);
    expect(grafanaService).toBeTruthy();
    expect(grafanaService?.spec?.type).toBe('ClusterIP');
  });

  it('should have grafana image digest', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.grafana.image.digest=fakedigest'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const grafanaDeployment = findDeployment(yaml, GRAFANA_NAME);
    expect(grafanaDeployment).toBeTruthy();
    const grafanaContainer = deploymentContainer(grafanaDeployment, GRAFANA_NAME);
    expect(grafanaContainer?.image).toContain('springcloud/spring-cloud-dataflow-grafana-prometheus@fakedigest');
  });

  it('should have grafana with load balancer', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.grafana.image.tag=1.2.3',
        'scdf.feature.monitoring.grafana.service.type=LoadBalancer'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const grafanaService = findService(yaml, GRAFANA_NAME);
    expect(grafanaService).toBeTruthy();
    expect(grafanaService?.spec?.type).toBe('LoadBalancer');
  });

  it('should have prometheus', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.prometheus.enabled=true',
        'scdf.feature.monitoring.prometheus.image.tag=1.2.3'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const prometheusDeployment = findDeployment(yaml, PROMETHEUS_NAME);
    expect(prometheusDeployment).toBeTruthy();
    const prometheusContainer = deploymentContainer(prometheusDeployment, PROMETHEUS_NAME);
    expect(prometheusContainer?.image).toContain('prom/prometheus:1.2.3');
  });

  it('should have prometheus image digest', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.prometheus.enabled=true',
        'scdf.feature.monitoring.prometheus.image.digest=fakedigest'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const prometheusDeployment = findDeployment(yaml, PROMETHEUS_NAME);
    expect(prometheusDeployment).toBeTruthy();
    const prometheusContainer = deploymentContainer(prometheusDeployment, PROMETHEUS_NAME);
    expect(prometheusContainer?.image).toContain('prom/prometheus@fakedigest');
  });

  it('should have prometheus-rsocket-proxy', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const prometheusRsocketProxyDeployment = findDeployment(yaml, PROMETHEUS_RSOCKET_PROXY_NAME);
    expect(prometheusRsocketProxyDeployment).toBeTruthy();
    const prometheusRsocketProxyContainer = deploymentContainer(
      prometheusRsocketProxyDeployment,
      PROMETHEUS_RSOCKET_PROXY_NAME
    );
    expect(prometheusRsocketProxyContainer?.image).toContain('micrometermetrics/prometheus-rsocket-proxy:1.0.0');
  });

  it('should have prometheus-rsocket-proxy image digest', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.image.digest=fakedigest'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const prometheusRsocketProxyDeployment = findDeployment(yaml, PROMETHEUS_RSOCKET_PROXY_NAME);
    expect(prometheusRsocketProxyDeployment).toBeTruthy();
    const prometheusRsocketProxyContainer = deploymentContainer(
      prometheusRsocketProxyDeployment,
      PROMETHEUS_RSOCKET_PROXY_NAME
    );
    expect(prometheusRsocketProxyContainer?.image).toContain('micrometermetrics/prometheus-rsocket-proxy@fakedigest');
  });
});
