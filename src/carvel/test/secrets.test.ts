import 'jest-extended';
import { DEFAULT_REQUIRED_DATA_VALUES } from '../src/constants';
import { findPodSpecsWithImagePullSecrets, findSecret } from '../src/k8s-helper';
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
    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(0);

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeTruthy();
  });

  it('should add carvel secretgen on default 2', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=mysql',
        'scdf.deploy.binder.type=kafka'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;
    const pods = findPodSpecsWithImagePullSecrets(yaml);
    expect(pods).toHaveLength(0);

    const secret = findSecret(yaml, 'reg-creds');
    expect(secret).toBeTruthy();
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
        'scdf.deploy.database.type=mysql',
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
});
