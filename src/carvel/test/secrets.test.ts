import 'jest-extended';
import { SCDF_SERVER_NAME, DEFAULT_REQUIRED_DATA_VALUES } from '../src/constants';
import { findDeployment, deploymentContainer, findPodSpecsWithImagePullSecrets, findSecret } from '../src/k8s-helper';

import { execYtt } from '../src/ytt';

describe('secrets', () => {


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
        'scdf.binder.type=rabbit',
        'scdf.registry.secret.ref=fakeref'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(2);

    // should just have fakeref and not any other defaults
    const refs = pods.flatMap(p => p.imagePullSecrets?.[0].name);
    expect(refs).toHaveLength(2);
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
