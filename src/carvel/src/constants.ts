export const BINDER_KAFKA_NAME = 'kafka';
export const BINDER_RABBIT_NAME = 'rabbitmq';
export const DB_MYSQL_NAME = 'mysql';
export const DB_POSTGRES_NAME = 'postgres';
export const DB_SKIPPER_NAME = 'db-skipper';
export const DB_DATAFLOW_NAME = 'db-dataflow';
export const SCDF_SERVER_NAME = 'scdf-server';
export const SKIPPER_NAME = 'skipper';
export const GRAFANA_NAME = 'grafana';
export const PROMETHEUS_NAME = 'prometheus';
export const PROMETHEUS_RSOCKET_PROXY_NAME = 'prometheus-rsocket-proxy';
export const DEFAULT_REQUIRED_DATA_VALUES = [
  'scdf.server.image.tag=2.8.1',
  'scdf.skipper.image.tag=2.7.1',
  'scdf.ctr.image.tag=2.8.1'
];
