import {
  listCalendarEvents,
  createCalendarEvent,
  updateCalendarEvent,
  deleteCalendarEvent,
  listInventoryItems,
  createInventoryItem,
  updateInventoryItem,
  deleteInventoryItem,
  listGoals,
  createGoal,
  updateGoal,
  deleteGoal,
  refreshProjections,
  getDashboard,
} from './household';

jest.mock('./client', () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  patch: jest.fn(),
  del: jest.fn(),
}));

const { get, post, put, patch, del } = require('./client');

beforeEach(() => jest.clearAllMocks());

describe('listCalendarEvents', () => {
  it('builds URL with all query params', () => {
    get.mockResolvedValue({ calendar_events: [] });
    listCalendarEvents('profile-1', 'BIRTHDAY', 1, 10);
    expect(get).toHaveBeenCalledWith(
      '/v1/household/calendar-events?profile_id=profile-1&event_type=BIRTHDAY&page=1&size=10'
    );
  });

  it('calls base URL when all params are null', () => {
    get.mockResolvedValue({ calendar_events: [] });
    listCalendarEvents(null, null, null, null);
    expect(get).toHaveBeenCalledWith('/v1/household/calendar-events');
  });
});

describe('createCalendarEvent', () => {
  it('calls post with correct endpoint and data', () => {
    post.mockResolvedValue({});
    const data = { title: 'Trip' };
    createCalendarEvent(data);
    expect(post).toHaveBeenCalledWith('/v1/household/calendar-events', data);
  });
});

describe('updateCalendarEvent', () => {
  it('calls patch with correct path and data', () => {
    patch.mockResolvedValue({});
    const data = { title: 'Updated Trip' };
    updateCalendarEvent('evt-1', data);
    expect(patch).toHaveBeenCalledWith('/v1/household/calendar-events/evt-1', data);
  });
});

describe('deleteCalendarEvent', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deleteCalendarEvent('evt-1');
    expect(del).toHaveBeenCalledWith('/v1/household/calendar-events/evt-1');
  });
});

describe('listInventoryItems', () => {
  it('builds URL with all query params', () => {
    get.mockResolvedValue({ inventory_items: [] });
    listInventoryItems('profile-1', 'AMAZON', 0, 20);
    expect(get).toHaveBeenCalledWith(
      '/v1/household/inventory-items?profile_id=profile-1&source_platform=AMAZON&page=0&size=20'
    );
  });

  it('calls base URL when all params are null', () => {
    get.mockResolvedValue({ inventory_items: [] });
    listInventoryItems(null, null, null, null);
    expect(get).toHaveBeenCalledWith('/v1/household/inventory-items');
  });
});

describe('createInventoryItem', () => {
  it('calls post with correct endpoint and data', () => {
    post.mockResolvedValue({});
    const data = { item_name: 'Milk' };
    createInventoryItem(data);
    expect(post).toHaveBeenCalledWith('/v1/household/inventory-items', data);
  });
});

describe('updateInventoryItem', () => {
  it('calls put with correct path and data', () => {
    put.mockResolvedValue({});
    const data = { item_name: 'Milk', is_consumed: true };
    updateInventoryItem('item-1', data);
    expect(put).toHaveBeenCalledWith('/v1/household/inventory-items/item-1', data);
  });
});

describe('deleteInventoryItem', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deleteInventoryItem('item-1');
    expect(del).toHaveBeenCalledWith('/v1/household/inventory-items/item-1');
  });
});

describe('listGoals', () => {
  it('builds URL with all query params', () => {
    get.mockResolvedValue({ goals: [] });
    listGoals('profile-1', 'IN_PROGRESS', 0, 10);
    expect(get).toHaveBeenCalledWith(
      '/v1/household/goals?profile_id=profile-1&status=IN_PROGRESS&page=0&size=10'
    );
  });

  it('calls base URL when all params are null', () => {
    get.mockResolvedValue({ goals: [] });
    listGoals(null, null, null, null);
    expect(get).toHaveBeenCalledWith('/v1/household/goals');
  });
});

describe('createGoal', () => {
  it('calls post with correct endpoint and data', () => {
    post.mockResolvedValue({});
    const data = { goal_name: 'Save for car' };
    createGoal(data);
    expect(post).toHaveBeenCalledWith('/v1/household/goals', data);
  });
});

describe('updateGoal', () => {
  it('calls patch with correct path and data', () => {
    patch.mockResolvedValue({});
    const data = { current_amount: 5000 };
    updateGoal('goal-1', data);
    expect(patch).toHaveBeenCalledWith('/v1/household/goals/goal-1', data);
  });
});

describe('deleteGoal', () => {
  it('calls del with correct path', () => {
    del.mockResolvedValue(null);
    deleteGoal('goal-1');
    expect(del).toHaveBeenCalledWith('/v1/household/goals/goal-1');
  });
});

describe('refreshProjections', () => {
  it('calls post with profile id in path', () => {
    post.mockResolvedValue({});
    refreshProjections('profile-1');
    expect(post).toHaveBeenCalledWith('/v1/projections/refresh/profile-1');
  });
});

describe('getDashboard', () => {
  it('calls get with profile id in path', () => {
    get.mockResolvedValue({});
    getDashboard('profile-1');
    expect(get).toHaveBeenCalledWith('/v1/projections/dashboard/profile-1');
  });
});
