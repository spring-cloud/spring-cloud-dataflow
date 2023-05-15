// @ts-ignore
import lodash from 'lodash';
import { execYtt } from '../src/ytt';
import { findDeployment, findConfigMap, findSecret, deploymentContainer, parseYamlDocument } from '../src/k8s-helper';
import {
  DB_MARIADB_NAME,
  DB_POSTGRES_NAME,
  DB_SKIPPER_NAME,
  DB_DATAFLOW_NAME,
  SCDF_SERVER_NAME,
  SKIPPER_NAME,
  DEFAULT_REQUIRED_DATA_VALUES
} from '../src/constants';

describe('databases', () => {
  it('should configure external db settings', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.server.database.url=fakeurl1',
        'scdf.server.database.username=fakeuser1',
        'scdf.server.database.password=fakepass1',
        'scdf.skipper.database.url=fakeurl2',
        'scdf.skipper.database.username=fakeuser2',
        'scdf.skipper.database.password=fakepass2'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const postgresSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperDeployment).toBeFalsy();

    const postgresDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowDeployment).toBeFalsy();

    const dataflowConfigMap = findConfigMap(yaml, SCDF_SERVER_NAME);
    const skipperConfigMap = findConfigMap(yaml, SKIPPER_NAME);
    const dataflowApplicationYaml = dataflowConfigMap?.data ? dataflowConfigMap.data['application.yaml'] : '';
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const dataflowDoc = parseYamlDocument(dataflowApplicationYaml);
    const dataflowJson = dataflowDoc.toJSON();
    const dataflowDatasourceUrl = lodash.get(dataflowJson, 'spring.datasource.url') as string;
    expect(dataflowDatasourceUrl).toBe('fakeurl1');
    const dataflowDatasourceUsername = lodash.get(dataflowJson, 'spring.datasource.username') as string;
    expect(dataflowDatasourceUsername).toBe('${external-user}');
    const dataflowDatasourcePassword = lodash.get(dataflowJson, 'spring.datasource.password') as string;
    expect(dataflowDatasourcePassword).toBe('${external-password}');

    const postgresDataflowSecret = findSecret(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowSecret).toBeTruthy();
    const postgresDataflowSecretData = postgresDataflowSecret?.data || {};
    expect(postgresDataflowSecretData['external-user']).toBe('ZmFrZXVzZXIx');
    expect(postgresDataflowSecretData['external-password']).toBe('ZmFrZXBhc3Mx');

    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();
    const skipperDatasourceUrl = lodash.get(skipperJson, 'spring.datasource.url') as string;
    expect(skipperDatasourceUrl).toBe('fakeurl2');
    const skipperDatasourceUsername = lodash.get(skipperJson, 'spring.datasource.username') as string;
    expect(skipperDatasourceUsername).toBe('${external-user}');
    const skipperDatasourcePassword = lodash.get(skipperJson, 'spring.datasource.password') as string;
    expect(skipperDatasourcePassword).toBe('${external-password}');

    const postgresSkipperSecret = findSecret(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperSecret).toBeTruthy();
    const postgresSkipperSecretData = postgresSkipperSecret?.data || {};
    expect(postgresSkipperSecretData['external-user']).toBe('ZmFrZXVzZXIy');
    expect(postgresSkipperSecretData['external-password']).toBe('ZmFrZXBhc3My');
  });
});
