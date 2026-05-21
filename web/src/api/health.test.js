import {
  createDoctorVisit,
  deleteDoctorVisit,
  deleteVital,
  listDoctorVisits,
  listVitals,
  recordVital,
  updateDoctorVisit,
} from './health';

jest.mock('./client', () => ({
  get: jest.fn(),
  post: jest.fn(),
  patch: jest.fn(),
  del: jest.fn(),
}));

const { get, post, patch, del } = require('./client');

beforeEach(() => jest.clearAllMocks());

describe('listVitals', () => {
  it('builds URL with profile_id and vital_type', () => {
    get.mockResolvedValue({ vitals: [] });
    listVitals('profile-1', 'WEIGHT');
    expect(get).toHaveBeenCalledWith('/v1/vitals?profile_id=profile-1&vital_type=WEIGHT');
  });

  it('omits vital_type when null', () => {
    get.mockResolvedValue({ vitals: [] });
    listVitals('profile-1', null);
    expect(get).toHaveBeenCalledWith('/v1/vitals?profile_id=profile-1');
  });

  it('calls base URL when both params are null', () => {
    get.mockResolvedValue({ vitals: [] });
    listVitals(null, null);
    expect(get).toHaveBeenCalledWith('/v1/vitals');
  });
});

describe('recordVital', () => {
  it('calls post with correct path and body', () => {
    post.mockResolvedValue({});
    const data = {
      profile_id: 'profile-1',
      vital_type: 'WEIGHT',
      reading_date: '2026-06-19',
      value_primary: 72,
      unit: 'kg',
    };
    recordVital(data);
    expect(post).toHaveBeenCalledWith('/v1/vitals', data);
  });
});

describe('deleteVital', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deleteVital('vital-123');
    expect(del).toHaveBeenCalledWith('/v1/vitals/vital-123');
  });
});

describe('listDoctorVisits', () => {
  it('builds URL with profile_id', () => {
    get.mockResolvedValue({ doctor_visits: [] });
    listDoctorVisits('profile-1');
    expect(get).toHaveBeenCalledWith('/v1/doctor-visits?profile_id=profile-1');
  });

  it('calls base URL when profileId is null', () => {
    get.mockResolvedValue({ doctor_visits: [] });
    listDoctorVisits(null);
    expect(get).toHaveBeenCalledWith('/v1/doctor-visits');
  });
});

describe('createDoctorVisit', () => {
  it('calls post with correct path and body', () => {
    post.mockResolvedValue({});
    const data = {
      profile_id: 'profile-1',
      from_date: '2026-06-19',
      visited_doctor: true,
      doctor_name: 'Dr. Smith',
    };
    createDoctorVisit(data);
    expect(post).toHaveBeenCalledWith('/v1/doctor-visits', data);
  });
});

describe('updateDoctorVisit', () => {
  it('calls patch with correct path and body', () => {
    patch.mockResolvedValue({});
    const data = { diagnosis: 'Flu', notes: 'Rest for 3 days' };
    updateDoctorVisit('visit-123', data);
    expect(patch).toHaveBeenCalledWith('/v1/doctor-visits/visit-123', data);
  });
});

describe('deleteDoctorVisit', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deleteDoctorVisit('visit-123');
    expect(del).toHaveBeenCalledWith('/v1/doctor-visits/visit-123');
  });
});
