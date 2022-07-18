import { execYtt } from '../src/ytt';

describe('errors', () => {
  it('should get error with missing options', async () => {
    const result = await execYtt({ files: ['config'] });
    expect(result.success).toBeFalsy();
    expect(result.stderr).toContain('Validation failed with following errors');
    expect(result.stderr).toContain('scdf.server.image.tag');
    expect(result.stderr).toContain('scdf.skipper.image.tag');
    expect(result.stderr).toContain('scdf.ctr.image.tag');
  });
});
