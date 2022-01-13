import 'jest-extended';
import { SCDF_SERVER_NAME, DEFAULT_REQUIRED_DATA_VALUES } from '../src/constants';
import { findDeployment, deploymentContainer, findPodSpecsWithImagePullSecrets, findSecret } from '../src/k8s-helper';

import { execYtt } from '../src/ytt';

describe('secrets', () => {
  it('should add carvel secretgen on default 1', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.deploy.binder.type=rabbit'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    // gh-4731
    // on default we need to have pod image pull secret
    // to ref to reg-creds which is basically no-op having
    // dump empty secret which is valid in terms of k8s
    // validation but just having nothing.
    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(5);

    // all default pull secrets need to ref to reg-creds
    const refs = pods.flatMap(p => p.imagePullSecrets?.[0].name);
    expect(refs).toHaveLength(5);
    expect(refs.every(r => r === 'reg-creds')).toBeTrue();

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeTruthy();
  });

  it('should add carvel secretgen on default 2', async () => {
    // see above test for as this is just same with different setup
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=mariadb',
        'scdf.deploy.binder.type=kafka'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(5);

    // all default pull secrets need to ref to reg-creds
    const refs = pods.flatMap(p => p.imagePullSecrets?.[0].name);
    expect(refs).toHaveLength(5);
    expect(refs.every(r => r === 'reg-creds')).toBeTrue();

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeTruthy();
  });

  it('should add carvel secretgen on default 3', async () => {
    // see above test for as this is just same with different setup
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.feature.monitoring.prometheus.enabled=true',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(8);

    // all default pull secrets need to ref to reg-creds
    const refs = pods.flatMap(p => p.imagePullSecrets?.[0].name);
    expect(refs).toHaveLength(8);
    expect(refs.every(r => r === 'reg-creds')).toBeTrue();

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeTruthy();
  });

  it('should add carvel secretgen on default 4', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    const volumeMount = dataflowContainer?.volumeMounts?.find(x => x.name === 'scdfmetadata');
    expect(volumeMount?.mountPath).toBe('/workspace/runtime/secrets');
    const volume = dataflowDeployment?.spec?.template.spec?.volumes?.find(x => x.name === 'scdfmetadata');
    expect(volume?.secret?.secretName).toBe('reg-creds');
  });

  it('should add manual image pull secret if defined 1', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.deploy.binder.type=rabbit',
        'scdf.registry.secret.ref=fakeref'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(5);

    // should just have fakeref and not any other defaults
    const refs = pods.flatMap(p => p.imagePullSecrets?.[0].name);
    expect(refs).toHaveLength(5);
    expect(refs.every(r => r === 'fakeref')).toBeTrue();

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeFalsy();
  });

  it('should add manual image pull secret if defined 2', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=mariadb',
        'scdf.deploy.binder.type=kafka',
        'scdf.registry.secret.ref=fakeref'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;
    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(5);

    const refs = pods.flatMap(p => p.imagePullSecrets?.[0].name);
    expect(refs).toHaveLength(5);
    expect(refs.every(r => r === 'fakeref')).toBeTrue();

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeFalsy();
  });

  it('should add manual image pull secret if defined 3', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.feature.monitoring.prometheus.enabled=true',
        'scdf.feature.monitoring.grafana.enabled=true',
        'scdf.feature.monitoring.prometheusRsocketProxy.enabled=true',
        'scdf.registry.secret.ref=fakeref'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;
    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(8);

    const refs = pods.flatMap(p => p.imagePullSecrets?.[0].name);
    expect(refs).toHaveLength(8);
    expect(refs.every(r => r === 'fakeref')).toBeTrue();

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeFalsy();
  });

  it('should add manual image pull secret if defined 4', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [...DEFAULT_REQUIRED_DATA_VALUES, 'scdf.registry.secret.ref=fakeref']
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const dataflowDeployment = findDeployment(yaml, SCDF_SERVER_NAME);
    const dataflowContainer = deploymentContainer(dataflowDeployment, SCDF_SERVER_NAME);
    const volumeMount = dataflowContainer?.volumeMounts?.find(x => x.name === 'scdfmetadata');
    expect(volumeMount?.mountPath).toBe('/workspace/runtime/secrets');
    const volume = dataflowDeployment?.spec?.template.spec?.volumes?.find(x => x.name === 'scdfmetadata');
    expect(volume?.secret?.secretName).toBe('fakeref');
  });
});
