import { execYtt } from '../src/ytt';

describe('generic', () => {
  it('should work with minimal config', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        'scdf.deploy.database.type=postgres',
        'scdf.server.image.tag=2.8.1',
        'scdf.skipper.image.tag=2.7.1',
        'scdf.ctr.image.tag=2.8.1'
      ]
    });
    expect(result.success, result.stderr).toBeTruthy();
  });
});
