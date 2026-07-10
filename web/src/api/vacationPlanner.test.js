import { checkVacationBudget } from './vacationPlanner';

jest.mock('./client', () => ({
  post: jest.fn(),
}));

const { post } = require('./client');

beforeEach(() => jest.clearAllMocks());

describe('checkVacationBudget', () => {
  it('calls post with profile_id query param and payload body', () => {
    post.mockResolvedValue({ within_budget: true });
    const payload = { trip_name: 'Goa Trip', budget: 50000 };
    checkVacationBudget('profile-1', payload);
    expect(post).toHaveBeenCalledWith(
      '/v1/vacation-planner/budget-check?profile_id=profile-1',
      payload
    );
  });
});
