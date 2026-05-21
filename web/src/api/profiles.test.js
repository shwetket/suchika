import {
  listProfiles,
  getProfile,
  createProfile,
  updateProfile,
  deactivateProfile,
} from './profiles';
import * as client from './client';

jest.mock('./client');

describe('profiles API', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('listProfiles', () => {
    it('calls get with base profiles URL when no params', () => {
      client.get.mockResolvedValue({ profiles: [] });
      listProfiles(null, null);
      expect(client.get).toHaveBeenCalledWith('/v1/profiles');
    });

    it('appends admin_id query param', () => {
      client.get.mockResolvedValue({ profiles: [] });
      listProfiles('admin-1', null);
      expect(client.get).toHaveBeenCalledWith('/v1/profiles?admin_id=admin-1');
    });

    it('appends is_active query param', () => {
      client.get.mockResolvedValue({ profiles: [] });
      listProfiles(null, true);
      expect(client.get).toHaveBeenCalledWith('/v1/profiles?is_active=true');
    });

    it('appends both params', () => {
      client.get.mockResolvedValue({ profiles: [] });
      listProfiles('admin-1', false);
      expect(client.get).toHaveBeenCalledWith('/v1/profiles?admin_id=admin-1&is_active=false');
    });

    it('returns data from get()', async () => {
      const mockData = { profiles: [{ profile_id: 'p1', full_name: 'Alice' }] };
      client.get.mockResolvedValue(mockData);
      const result = await listProfiles(null, null);
      expect(result).toEqual(mockData);
    });
  });

  describe('getProfile', () => {
    it('calls get with profile ID in path', () => {
      client.get.mockResolvedValue({ profile_id: 'p1' });
      getProfile('p1');
      expect(client.get).toHaveBeenCalledWith('/v1/profiles/p1');
    });
  });

  describe('createProfile', () => {
    it('calls post with profiles endpoint and data', () => {
      client.post.mockResolvedValue({ profile_id: 'new' });
      const data = { full_name: 'Bob', admin_id: 'a1' };
      createProfile(data);
      expect(client.post).toHaveBeenCalledWith('/v1/profiles', data);
    });
  });

  describe('updateProfile', () => {
    it('calls patch with profile ID path and data', () => {
      client.patch.mockResolvedValue({ profile_id: 'p1' });
      const data = { email_address: 'bob@example.com' };
      updateProfile('p1', data);
      expect(client.patch).toHaveBeenCalledWith('/v1/profiles/p1', data);
    });
  });

  describe('deactivateProfile', () => {
    it('calls del with profile ID path', () => {
      client.del.mockResolvedValue(null);
      deactivateProfile('p1');
      expect(client.del).toHaveBeenCalledWith('/v1/profiles/p1');
    });
  });
});
