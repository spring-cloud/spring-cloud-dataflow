import { execYtt } from '../src/ytt';
import { DEFAULT_REQUIRED_DATA_VALUES } from '../src/constants';

describe('errors', () => {
  it('should get error with missing options', async () => {
    const result = await execYtt({ files: ['config'] });
    expect(result.success, result.stderr).toBeFalsy();
    expect(result.stderr).toContain('Validation failed with following errors');
    expect(result.stderr).toContain('scdf.server.image.tag');
    expect(result.stderr).toContain('scdf.skipper.image.tag');
    expect(result.stderr).toContain('scdf.ctr.image.tag');
  });

  it('should get error with wrong db and binder', async () => {
    const result = await execYtt({
      files: ['config'],
      dataValueYamls: [
        ...DEFAULT_REQUIRED_DATA_VALUES,
        'scdf.binder.type=fake2'
      ]
    });
    expect(result.success, result.stderr).toBeFalsy();
    expect(result.stderr).toContain('Validation failed with following errors');
    expect(result.stderr).toContain('scdf.binder.type');
  });
});
