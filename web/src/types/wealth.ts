/**
 * Wealth Domain Types
 */

export interface Account {
  id: string;
  profileId: string;
  name: string;
  type: 'savings' | 'checking' | 'investment' | 'crypto';
  balance: number;
  currency: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface Transaction {
  id: string;
  accountId: string;
  amount: number;
  type: 'income' | 'expense' | 'transfer';
  category: string;
  description: string;
  date: Date;
  createdAt: Date;
  metadata?: Record<string, unknown>;
}

export interface TransactionFilter {
  accountId?: string;
  type?: string;
  category?: string;
  dateFrom?: Date;
  dateTo?: Date;
  limit?: number;
  offset?: number;
}

export interface WealthDashboard {
  totalBalance: number;
  monthlyIncome: number;
  monthlyExpense: number;
  savingsRate: number;
  accounts: Account[];
  recentTransactions: Transaction[];
}
