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
  it('should default to postgres', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValues: [...DEFAULT_REQUIRED_DATA_VALUES]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const postgresSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperDeployment).toBeTruthy();
    const postgresSkipperContainer = deploymentContainer(postgresSkipperDeployment, DB_POSTGRES_NAME);
    expect(postgresSkipperContainer?.image).toBe('postgres:10');
    const postgresSkipperSecret = findSecret(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperSecret).toBeTruthy();
    const postgresSkipperSecretData = postgresSkipperSecret?.data || {};
    expect(postgresSkipperSecretData['postgres-user']).toBe('ZGF0YWZsb3c=');
    expect(postgresSkipperSecretData['postgres-password']).toBe('c2VjcmV0');

    const postgresDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowDeployment).toBeTruthy();
    const postgresDataflowContainer = deploymentContainer(postgresDataflowDeployment, DB_POSTGRES_NAME);
    expect(postgresDataflowContainer?.image).toBe('postgres:10');
    const postgresDataflowSecret = findSecret(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowSecret).toBeTruthy();
    const postgresDataflowSecretData = postgresDataflowSecret?.data || {};
    expect(postgresDataflowSecretData['postgres-user']).toBe('ZGF0YWZsb3c=');
    expect(postgresDataflowSecretData['postgres-password']).toBe('c2VjcmV0');
  });

  it('should use postgres digest images', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValues: [...DEFAULT_REQUIRED_DATA_VALUES, 'scdf.deploy.database.postgres.image.digest=fakedigest']
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const postgresSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperDeployment).toBeTruthy();
    const postgresSkipperContainer = deploymentContainer(postgresSkipperDeployment, DB_POSTGRES_NAME);
    expect(postgresSkipperContainer?.image).toBe('postgres@fakedigest');

    const postgresDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowDeployment).toBeTruthy();
    const postgresDataflowContainer = deploymentContainer(postgresDataflowDeployment, DB_POSTGRES_NAME);
    expect(postgresDataflowContainer?.image).toBe('postgres@fakedigest');
  });

  it('should deploy mariadb', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValues: [...DEFAULT_REQUIRED_DATA_VALUES, 'scdf.deploy.database.type=mariadb']
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const mariadbSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(mariadbSkipperDeployment).toBeTruthy();
    const mariadbSkipperContainer = deploymentContainer(mariadbSkipperDeployment, DB_MARIADB_NAME);
    expect(mariadbSkipperContainer?.image).toContain('mariadb');
    const mariadbSkipperSecret = findSecret(yaml, DB_SKIPPER_NAME);
    expect(mariadbSkipperSecret).toBeTruthy();
    const mariadbSkipperSecretData = mariadbSkipperSecret?.data || {};
    expect(mariadbSkipperSecretData['mariadb-user']).toBe('ZGF0YWZsb3c=');
    expect(mariadbSkipperSecretData['mariadb-root-password']).toBe('c2VjcmV0');

    const mariadbDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(mariadbDataflowDeployment).toBeTruthy();
    const mariadbDataflowContainer = deploymentContainer(mariadbDataflowDeployment, DB_MARIADB_NAME);
    expect(mariadbDataflowContainer?.image).toContain('mariadb');
    const mariadbDataflowSecret = findSecret(yaml, DB_DATAFLOW_NAME);
    expect(mariadbDataflowSecret).toBeTruthy();
    const mariadbDataflowSecretData = mariadbDataflowSecret?.data || {};
    expect(mariadbDataflowSecretData['mariadb-user']).toBe('ZGF0YWZsb3c=');
    expect(mariadbDataflowSecretData['mariadb-root-password']).toBe('c2VjcmV0');
  });

  it('should use mariadb digest images', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValues: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=mariadb',
        'scdf.deploy.database.mariadb.image.digest=fakedigest'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const mariadbSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(mariadbSkipperDeployment).toBeTruthy();
    const mariadbSkipperContainer = deploymentContainer(mariadbSkipperDeployment, DB_MARIADB_NAME);
    expect(mariadbSkipperContainer?.image).toBe('mariadb@fakedigest');

    const mariadbDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(mariadbDataflowDeployment).toBeTruthy();
    const mariadbDataflowContainer = deploymentContainer(mariadbDataflowDeployment, DB_MARIADB_NAME);
    expect(mariadbDataflowContainer?.image).toBe('mariadb@fakedigest');
  });

  it('should deploy postgres', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValues: [...DEFAULT_REQUIRED_DATA_VALUES, 'scdf.deploy.database.type=postgres']
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const postgresSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperDeployment).toBeTruthy();
    const postgresSkipperContainer = deploymentContainer(postgresSkipperDeployment, DB_POSTGRES_NAME);
    expect(postgresSkipperContainer?.image).toContain('postgres');
    const postgresSkipperSecret = findSecret(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperSecret).toBeTruthy();
    const postgresSkipperSecretData = postgresSkipperSecret?.data || {};
    expect(postgresSkipperSecretData['postgres-user']).toBe('ZGF0YWZsb3c=');
    expect(postgresSkipperSecretData['postgres-password']).toBe('c2VjcmV0');

    const postgresDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowDeployment).toBeTruthy();
    const postgresDataflowContainer = deploymentContainer(postgresDataflowDeployment, DB_POSTGRES_NAME);
    expect(postgresDataflowContainer?.image).toContain('postgres');
    const postgresDataflowSecret = findSecret(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowSecret).toBeTruthy();
    const postgresDataflowSecretData = postgresDataflowSecret?.data || {};
    expect(postgresDataflowSecretData['postgres-user']).toBe('ZGF0YWZsb3c=');
    expect(postgresDataflowSecretData['postgres-password']).toBe('c2VjcmV0');
  });

  it('should deploy mariadb with custom username and password', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValues: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=mariadb',
        'scdf.deploy.database.mariadb.username=user',
        'scdf.deploy.database.mariadb.password=pass'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const mariadbSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(mariadbSkipperDeployment).toBeTruthy();
    const mariadbSkipperContainer = deploymentContainer(mariadbSkipperDeployment, DB_MARIADB_NAME);
    expect(mariadbSkipperContainer?.image).toContain('mariadb');
    const mariadbSkipperSecret = findSecret(yaml, DB_SKIPPER_NAME);
    expect(mariadbSkipperSecret).toBeTruthy();
    const mariadbSkipperSecretData = mariadbSkipperSecret?.data || {};
    expect(mariadbSkipperSecretData['mariadb-user']).toBe('user');
    expect(mariadbSkipperSecretData['mariadb-root-password']).toBe('pass');

    const mariadbDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(mariadbDataflowDeployment).toBeTruthy();
    const mariadbDataflowContainer = deploymentContainer(mariadbDataflowDeployment, DB_MARIADB_NAME);
    expect(mariadbDataflowContainer?.image).toContain('mariadb');
    const mariadbDataflowSecret = findSecret(yaml, DB_DATAFLOW_NAME);
    expect(mariadbDataflowSecret).toBeTruthy();
    const mariadbDataflowSecretData = mariadbDataflowSecret?.data || {};
    expect(mariadbDataflowSecretData['mariadb-user']).toBe('user');
    expect(mariadbDataflowSecretData['mariadb-root-password']).toBe('pass');
  });

  it('should deploy postgres with custom username and password', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValues: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.type=postgres',
        'scdf.deploy.database.postgres.username=user',
        'scdf.deploy.database.postgres.password=pass'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const postgresSkipperDeployment = findDeployment(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperDeployment).toBeTruthy();
    const postgresSkipperContainer = deploymentContainer(postgresSkipperDeployment, DB_POSTGRES_NAME);
    expect(postgresSkipperContainer?.image).toContain('postgres');
    const postgresSkipperSecret = findSecret(yaml, DB_SKIPPER_NAME);
    expect(postgresSkipperSecret).toBeTruthy();
    const postgresSkipperSecretData = postgresSkipperSecret?.data || {};
    expect(postgresSkipperSecretData['postgres-user']).toBe('user');
    expect(postgresSkipperSecretData['postgres-password']).toBe('pass');

    const postgresDataflowDeployment = findDeployment(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowDeployment).toBeTruthy();
    const postgresDataflowContainer = deploymentContainer(postgresDataflowDeployment, DB_POSTGRES_NAME);
    expect(postgresDataflowContainer?.image).toContain('postgres');
    const postgresDataflowSecret = findSecret(yaml, DB_DATAFLOW_NAME);
    expect(postgresDataflowSecret).toBeTruthy();
    const postgresDataflowSecretData = postgresDataflowSecret?.data || {};
    expect(postgresDataflowSecretData['postgres-user']).toBe('user');
    expect(postgresDataflowSecretData['postgres-password']).toBe('pass');
  });

  it('should configure external db settings', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.deploy.database.enabled=false',
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
