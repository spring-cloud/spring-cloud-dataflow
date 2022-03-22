import { parseDocument, parseAllDocuments, Document } from 'yaml';
import {
  loadYaml,
  V1Deployment,
  V1Container,
  V1ConfigMap,
  V1Secret,
  V1EnvVar,
  V1Service,
  V1StatefulSet,
  V1VolumeMount,
  V1Volume,
  V1PodSpec
} from '@kubernetes/client-node';

export function parseDocuments(yaml: string): string[] {
  return parseAllDocuments(yaml).map(d => d.toString());
}

export function parseYamlDocument(yaml: string): Document.Parsed {
  return parseDocument(yaml);
}

export function findAnnotation(node: V1Service | V1Deployment | undefined, name: string): string | undefined {
  return node?.metadata?.annotations ? node?.metadata?.annotations[name] : undefined;
}

export function findAnnotations(node: V1Service | V1Deployment | undefined, name: string): string[] {
  let values: string[] = [];
  const annotations = node?.metadata?.annotations;
  if (annotations) {
    for (const k in annotations) {
      if (k.includes(name)) {
        const v = annotations[k];
        values.push(v);
      }
    }
  }
  return values;
}

export function findDeployment(yaml: string, name: string): V1Deployment | undefined {
  return parseDocuments(yaml)
    .map(d => loadYaml<V1Deployment>(d))
    .find(node => {
      if (node?.kind === 'Deployment' && node?.metadata?.name === name) {
        return node;
      }
    });
}

export function findStatefulSet(yaml: string, name: string): V1StatefulSet | undefined {
  return parseDocuments(yaml)
    .map(d => loadYaml<V1StatefulSet>(d))
    .find(node => {
      if (node?.kind === 'StatefulSet' && node?.metadata?.name === name) {
        return node;
      }
    });
}

export function findService(yaml: string, name: string): V1Service | undefined {
  return parseDocuments(yaml)
    .map(d => loadYaml<V1Service>(d))
    .find(node => {
      if (node?.kind === 'Service' && node?.metadata?.name === name) {
        return node;
      }
    });
}

export function findConfigMap(yaml: string, name: string): V1ConfigMap | undefined {
  return parseDocuments(yaml)
    .map(d => loadYaml<V1ConfigMap>(d))
    .find(node => {
      if (node?.kind === 'ConfigMap' && node?.metadata?.name === name) {
        return node;
      }
    });
}

export function findSecret(yaml: string, name: string): V1Secret | undefined {
  return parseDocuments(yaml)
    .map(d => loadYaml<V1Secret>(d))
    .find(node => {
      if (node?.kind === 'Secret' && node?.metadata?.name === name) {
        return node;
      }
    });
}

export function findPodSpecsWithImagePullSecrets(yaml: string): V1PodSpec[] {
  const pods: V1PodSpec[] = [];
  parseDocuments(yaml)
    .map(d => loadYaml<V1Deployment>(d))
    .filter(node => node?.kind === 'Deployment' && node?.spec?.template?.spec?.imagePullSecrets)
    .forEach(deployment => {
      if (deployment.spec?.template?.spec) {
        pods.push(deployment.spec?.template?.spec);
      }
    });
  parseDocuments(yaml)
    .map(d => loadYaml<V1StatefulSet>(d))
    .filter(node => node?.kind === 'StatefulSet' && node?.spec?.template?.spec?.imagePullSecrets)
    .forEach(ss => {
      if (ss.spec?.template?.spec) {
        pods.push(ss.spec?.template?.spec);
      }
    });
  return pods;
}

export function deploymentContainer(deployment: V1Deployment | undefined, name: string): V1Container | undefined {
  return deployment?.spec?.template?.spec?.containers.find(container => container.name === name);
}

export function deploymentVolume(deployment: V1Deployment | undefined, name: string): V1Volume | undefined {
  return deployment?.spec?.template?.spec?.volumes?.find(volume => volume.name === name);
}

export function statefulSetContainer(ss: V1StatefulSet | undefined, name: string): V1Container | undefined {
  return ss?.spec?.template?.spec?.containers.find(container => container.name === name);
}

export function containerEnvValue(container: V1Container | undefined, name: string): string | undefined {
  return container?.env?.find(env => env.name === name)?.value;
}

export function containerEnvValues(container: V1Container | undefined): V1EnvVar[] | undefined {
  return container?.env;
}

export function containerVolumeMounts(container: V1Container | undefined): V1VolumeMount[] | undefined {
  return container?.volumeMounts;
}

export function containerVolumeMount(container: V1Container | undefined, name: string): V1VolumeMount | undefined {
  return container?.volumeMounts?.find(vm => vm.name === name);
}

export function envStringToMap(envString: string): Map<string, string> {
  return envString.split(',').reduce((envMap, cur) => {
    const s = cur.split('=', 2).map(e => e.trim());
    if (s.length === 2) {
      envMap.set(s[0], s[1]);
    }
    return envMap;
  }, new Map<string, string>());
}
