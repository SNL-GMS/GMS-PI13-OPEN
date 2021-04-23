import { createConsumer, createKafka } from '../src/ts/kafka/kafka';
import { KafkaConsumer } from '../src/ts/kafka/kafka-consumer';
import { KafkaStatus } from '../src/ts/kafka/types';

const kafkaSettings: any = {
  clientId: 'api-gateway',
  groupId: 'user-interface',
  brokers: ['kafka:9092'],
  connectionTimeout: 3000,
  maxWaitTimeInMs: 50,
  heartbeatInterval: 500, // ms
  consumerTopics: {
    systemMessagesTopic: 'system.system-messages',
    uiStationSoh: 'soh.ui-materialized-view'
  },
  producerTopics: {
    acknowledgedTopic: 'soh.ack-station-soh',
    quietedTopic: 'soh.quieted-list'
  }
};

describe('kafka consumer ', () => {
  test('can be imported', () => {
    const kafkaConsumer: KafkaConsumer = KafkaConsumer.createKafkaConsumer(kafkaSettings, [
      'topic'
    ]);
    expect(kafkaConsumer.getStatus()).toEqual(KafkaStatus.NOT_INITIALIZED);
  });

  test('initialize can handle errors', async () => {
    const kafka = createKafka('clientId', ['broker']);
    createConsumer(
      kafka,
      kafkaSettings.groupId,
      kafkaSettings.maxWaitTimeInMs,
      kafkaSettings.heartbeatInterval
    );

    const kafkaConsumer: KafkaConsumer = KafkaConsumer.createKafkaConsumer(kafkaSettings, [
      'topic'
    ]);

    kafkaConsumer.start().catch();
    expect(kafkaConsumer.getStatus()).not.toEqual(KafkaStatus.NOT_INITIALIZED);
    await kafkaConsumer.stop();
  });
});
