// @ts-ignore
import lodash from 'lodash';
import 'jest-extended';
import { execYtt } from '../src/ytt';
import {
  findDeployment,
  findConfigMap,
  parseYamlDocument,
  envStringToMap,
  deploymentVolume,
  deploymentContainer,
  containerVolumeMount,
  findService
} from '../src/k8s-helper';
import { BINDER_RABBIT_NAME, BINDER_KAFKA_NAME, DEFAULT_REQUIRED_DATA_VALUES, SKIPPER_NAME } from '../src/constants';

describe('binders rabbit', () => {
  it('should skip deploy if external settings', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=rabbit',
        'scdf.binder.rabbit.host=localhost',
        'scdf.binder.rabbit.port=1234',
        'scdf.binder.rabbit.username=user',
        'scdf.binder.rabbit.password=pass'
      ]
    });

    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const kafkaDeployment = findDeployment(yaml, `${BINDER_KAFKA_NAME}-zk`);
    expect(kafkaDeployment).toBeFalsy();

    const rabbitDeployment = findDeployment(yaml, BINDER_RABBIT_NAME);
    expect(rabbitDeployment).toBeFalsy();

    const skipperConfigMap = findConfigMap(yaml, 'skipper');
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();
    const platformDefEnv = lodash.get(
      skipperJson,
      'spring.cloud.skipper.server.platform.kubernetes.accounts.default.environmentVariables'
    ) as string;
    const envs = envStringToMap(platformDefEnv);
    expect(envs.get('SPRING_RABBITMQ_HOST')).toBe('localhost');
    expect(envs.get('SPRING_RABBITMQ_PORT')).toBe('1234');
    expect(envs.get('SPRING_RABBITMQ_USERNAME')).toBe('user');
    expect(envs.get('SPRING_RABBITMQ_PASSWORD')).toBe('pass');
  });
});

describe('binders kafka', () => {
  it('should configure for kafka if external', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=broker',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=zk',
        'scdf.binder.kafka.zk.port=1235'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const skipperConfigMap = findConfigMap(yaml, SKIPPER_NAME);
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';
    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();

    const platformDefEnv = lodash.get(
      skipperJson,
      'spring.cloud.skipper.server.platform.kubernetes.accounts.default.environmentVariables'
    ) as string;
    const envs = envStringToMap(platformDefEnv);
    expect(envs.get('SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS')).toBe('broker:1234');
    expect(envs.get('SPRING_CLOUD_STREAM_KAFKA_BINDER_ZK_NODES')).toBe('zk:1235');
  });

  it('should skip deploy if external settings', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=kafka',
        'scdf.binder.kafka.broker.host=broker',
        'scdf.binder.kafka.broker.port=1234',
        'scdf.binder.kafka.zk.host=zk',
        'scdf.binder.kafka.zk.port=1235'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
    const yaml = result.stdout;

    const kafkaDeployment = findDeployment(yaml, `${BINDER_KAFKA_NAME}-zk`);
    expect(kafkaDeployment).toBeFalsy();

    const rabbitDeployment = findDeployment(yaml, BINDER_RABBIT_NAME);
    expect(rabbitDeployment).toBeFalsy();

    const skipperConfigMap = findConfigMap(yaml, 'skipper');
    const skipperApplicationYaml = skipperConfigMap?.data ? skipperConfigMap.data['application.yaml'] : '';

    const skipperDoc = parseYamlDocument(skipperApplicationYaml);
    const skipperJson = skipperDoc.toJSON();
    const platformDefEnv = lodash.get(
      skipperJson,
      'spring.cloud.skipper.server.platform.kubernetes.accounts.default.environmentVariables'
    ) as string;
    const envs = envStringToMap(platformDefEnv);
    expect(envs.get('SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS')).toBe('broker:1234');
  });
});
