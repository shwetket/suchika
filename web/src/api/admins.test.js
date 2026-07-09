import { listAdmins, createAdmin } from './admins';

jest.mock('./client', () => ({
  get: jest.fn(),
  post: jest.fn(),
}));

const { get, post } = require('./client');

beforeEach(() => jest.clearAllMocks());

describe('listAdmins', () => {
  it('calls get with the admins endpoint', () => {
    get.mockResolvedValue({ admins: [] });
    listAdmins();
    expect(get).toHaveBeenCalledWith('/v1/admins');
  });
});

describe('createAdmin', () => {
  it('calls post with the admins endpoint and body', () => {
    post.mockResolvedValue({ admin_id: 'a1' });
    const data = { full_name: 'Admin User', email_address: 'admin@example.com' };
    createAdmin(data);
    expect(post).toHaveBeenCalledWith('/v1/admins', data);
  });
});
