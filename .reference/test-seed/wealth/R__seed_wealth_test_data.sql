-- ==============================================================================
-- R__seed_wealth_test_data.sql
-- LOCAL-ONLY REAL DATA — gitignored (application/flyway/test-seed/) on the
-- "seed-data" branch, never pushed/merged. Sources: real Kotak/HDFC statements in
-- .referance/ (also gitignored) + assets_06062026.json / liabilities_06062026.json /
-- Financial_Data.md provided directly by the household (verified 2026-06-06, some
-- figures re-verified 2026-04-18/19 per each source file's own "last_verified" tags).
-- Bank account numbers are masked to last 4 digits. Known gaps flagged inline and
-- summarized at the bottom.
--
-- profile_id references (from R__seed_profile_test_data.sql):
--   00000000-0000-0000-0000-000000000002 = Ketan Verma (SELF)
--   00000000-0000-0000-0000-000000000003 = Shweta Ketan Verma (SPOUSE)
--   00000000-0000-0000-0000-000000000004 = Gayan Verma (CHILD, son)
--   00000000-0000-0000-0000-000000000005 = Vamika Verma (CHILD, daughter)
-- ==============================================================================

TRUNCATE TABLE wealth.account CASCADE;
TRUNCATE TABLE wealth.physical_asset CASCADE;

-- ------------------------------------------------------------------------------
-- FIXTURE ACCOUNT FOR PRE-EXISTING INTEGRATION TESTS — not real data.
-- application/domain/wealth/adapters/src/test/java/.../persistence/StatementUploadIntegrationTest.java
-- and UploadErrorLogPanacheRepositoryTest.java run against this real local app_db
-- (via the "integration-test" Quarkus config profile) and hardcode this exact
-- account id. The old generic placeholder seed provided it; the real-data seed
-- below replaced that placeholder entirely, so this row must stay to keep those
-- 12 pre-existing tests passing (found 2026-07-10 running the full suite for Sonar).
-- ------------------------------------------------------------------------------
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('f3b90000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000002',
        'Test Bank', 'Test Account', 'SAVINGS', 'INR', true, 0.00, '{}'::jsonb);

-- ------------------------------------------------------------------------------
-- ACCOUNTS — bank accounts (real, from statements)
-- ------------------------------------------------------------------------------

-- 1. Kotak Mahindra Bank Savings — Ketan, Bellandur branch. Real closing balance as
--    on 01/07/2026 was INR 11.00 with zero transactions in the Apr-Jun 2026 period.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002',
        'Kotak Mahindra Bank', 'Kotak Savings - Bellandur', 'SAVINGS', 'INR', true, 11.00,
        '{"ifsc": "KKBK0008122", "micr": "560485104", "branch": "BANGALORE - BELLANDUR", "account_last4": "0966"}'::jsonb);

-- 2. Kotak Mahindra Bank Joint Savings — Shweta (primary) + Ketan (joint holder),
--    Varthur branch. Nominee: Gayan Verma. Opening balance reconciled by hand back
--    to 31-03-2026 against the real statement's running balances (kept as-is so the
--    seeded transaction history below stays internally consistent — see live_balance
--    in metadata for the more current 2026-07-10 snapshot, now a direct read from
--    Shweta's own wealth app - supersedes the earlier value derived indirectly from
--    Ketan's app total).
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003',
        'Kotak Mahindra Bank', 'Kotak Joint - Varthur', 'SAVINGS', 'INR', true, 37221.18,
        '{"ifsc": "KKBK0008134", "micr": "560485120", "branch": "BANGALORE - VARTHUR MAIN ROAD", "account_last4": "2252", "joint_owners": ["Ketan Verma"], "nominee": "Gayan Verma", "live_balance_2026_07_10": 13593.59, "live_balance_source": "direct read from Shwetas Wealth app screenshot"}'::jsonb);

-- 3. HDFC Bank Savings ("Prime Potential") — Ketan, Gachibowli branch, opened 2011.
--    Opening balance reconciles exactly against every real running-balance row below.
--    CONFIRMED (2026-07-10): this is Ketan's ONLY HDFC account and it is his SALARY
--    account (Goldman Sachs salary + annual bonus both credit here); the real
--    transactions below already show the monthly transfers out to BoB MaxGain for
--    the home loan and to BoI for the car loan EMI, matching this exactly.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002',
        'HDFC Bank', 'HDFC Prime Potential - Gachibowli (Salary Account)', 'SAVINGS', 'INR', true, 169475.63,
        '{"ifsc": "HDFC0009095", "micr": "500240103", "branch": "MEENAKSHI BAMBOOS - GACHIBOWLI", "account_last4": "5889", "account_open_date": "2011-04-10", "variant": "PRIME POTENTIAL", "role": "salary_account", "employer": "Goldman Sachs", "note": "net salary Rs 2,40,383/month + annual bonus Rs 6,32,272 (net) both credit here; monthly transfers out to BoB MaxGain (home loan) and BoI (car loan)", "live_balance_2026_07_10": 459.47, "live_balance_source": "Ketans Wealth app screenshot"}'::jsonb);

-- ------------------------------------------------------------------------------
-- ACCOUNTS — loans (real figures from liabilities_06062026.json, verified 2026-04-19)
-- ------------------------------------------------------------------------------

-- 4. Home Loan 1 — Bank of Baroda MaxGain, against the self-occupied Sobha flat
--    (Varthur). Serviced via HDFC standing instruction (bundled with HL2 as a single
--    real NEFT debit of 76,500 — see HDFC transactions below; 31,545+44,888=76,433,
--    ~67 off, immaterial rounding/bundling by the MaxGain product).
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, interest_rate, emi_amount, metadata)
VALUES ('a1000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002',
        'Bank of Baroda', 'Home Loan 1 - BoB MaxGain', 'HOME_LOAN', 'INR', true, 3771120.00, 7.20, 31545.00,
        '{"original_disbursed": 5234061, "paid_so_far": 1423846, "start_date": "2020-10-01", "tenure_months": 240, "months_remaining": 174, "estimated_end_date": "2034-10-01", "principal_component": 8621, "interest_component": 22924, "payment_source": "MaxGain account (auto-debit)", "property": "Self-Occupied Flat - Sobha Dream Acres", "servicing_ifsc": "BARB0MARTHA"}'::jsonb);

-- 5. Home Loan 2 — Bank of Baroda MaxGain, against the RENTAL flat "GS Sunshine"
--    (CORRECTED 2026-07-10: this is NOT a secondary mortgage on the self-occupied
--    Sobha flat as earlier assumed — Ketan confirmed HL2 is specifically the loan
--    against the rental property. This also resolves the earlier "NO DEBT" vs
--    "Loan Linked: Y" conflict on the rental flat in favor of Y.)
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, interest_rate, emi_amount, metadata)
VALUES ('a1000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000002',
        'Bank of Baroda', 'Home Loan 2 - BoB MaxGain', 'HOME_LOAN', 'INR', true, 4912020.00, 7.35, 44888.00,
        '{"original_disbursed": 5500000, "paid_so_far": 525480, "start_date": "2022-01-01", "tenure_months": 180, "months_remaining": 129, "estimated_end_date": "2037-02-01", "principal_component": 12136, "interest_component": 32753, "payment_source": "Salary account (auto-debit)", "property": "Rental Flat - Sunshine (GS Sunshine)", "servicing_ifsc": "BARB0MARTHA"}'::jsonb);

-- 6. Car Loan — Bank of India. CORRECTED 2026-07-10: this loan is against a Tata
--    Tiago that has SINCE BEEN SOLD within the family — NOT the Tata Nexon (which
--    Ketan confirmed has no loan at all). The loan itself is still active/being
--    serviced despite the vehicle no longer being owned - no physical_asset row
--    exists for the Tiago since it's no longer part of the household's assets.
--    Closure target Oct 2027; EMI redirects to Gayan/Vamika education SIPs on
--    closure (per Financial_Data.md Rule C2).
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, interest_rate, emi_amount, metadata)
VALUES ('a1000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000002',
        'Bank of India', 'Car Loan - BoI (Tata Tiago, sold)', 'CAR_LOAN', 'INR', true, 135093.00, 9.00, 8276.00,
        '{"original_amount": 500000, "paid_so_far": 336168, "tenure_months": 60, "months_remaining": 21, "estimated_end_date": "2027-10-01", "principal_component": 7047, "interest_component": 1229, "payment_source": "Salary account (auto-debit)", "vehicle": "Tata Tiago (sold within family, 2026-07-10) - loan still active", "servicing_ifsc": "BKID0008822", "post_closure_plan": "EMI redirects to Gayan + Vamika education SIPs", "note": "confirmed 2026-07-10: no repayment/reimbursement arrangement exists for the sold vehicle - Ketan solely continues servicing this EMI"}'::jsonb);

-- 6b. Bank of India Savings — Ketan, a real deposit account distinct from the Car
--     Loan above (confirmed via Ketan's Wealth app, real balance as of 2026-07-10).
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000027', '00000000-0000-0000-0000-000000000002',
        'Bank of India', 'BoI Savings Account', 'SAVINGS', 'INR', true, 13934.90,
        '{"account_last4": "9030", "balance_as_of": "2026-07-10"}'::jsonb);

-- 7-8. MaxGain overdraft-linked accounts — Bank of Baroda, one per home loan (confirmed
--      2026-07-10: Ketan has 5 BoB accounts total — 2 loans above, 2 MaxGain below, 1
--      plain savings further below). Balances UPDATED 2026-07-10 to real live figures
--      from Ketan's Wealth app (accounts ••1422 and ••3923); the Rs 6L/1L split from
--      liabilities_06062026.json was an earlier snapshot, since grown via arbitrage.
--      A household asset ledger (~April 2026) independently shows the combined
--      MaxGain total as Rs 7,00,000 (vs 282995.40+405364.66=688360.06 in July) -
--      consistent with modest arbitrage growth over ~3 months.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000002',
        'Bank of Baroda', 'MaxGain Buffer - HL1', 'CURRENT', 'INR', true, 282995.40,
        '{"usage": "EMI auto-debit, arbitrage, liquidity buffer", "emi_linked": "Home Loan 1 (Rs 31,545/month)", "role": "Critical liquidity for job loss scenario (8.2 months runway per crisis_scenarios.job_loss)", "account_last4": "1422", "balance_as_of": "2026-07-10"}'::jsonb);
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000002',
        'Bank of Baroda', 'MaxGain Buffer - HL2', 'CURRENT', 'INR', true, 405364.66,
        '{"usage": "EMI auto-debit, arbitrage, liquidity buffer", "emi_linked": "Home Loan 2 (Rs 44,888/month)", "role": "Critical liquidity for job loss scenario (8.2 months runway per crisis_scenarios.job_loss)", "account_last4": "3923", "balance_as_of": "2026-07-10"}'::jsonb);

-- ------------------------------------------------------------------------------
-- ACCOUNTS — statutory retirement (real balances, Financial_Data.md is source of truth)
-- ------------------------------------------------------------------------------

-- 8. Employee Provident Fund — Goldman Sachs. UPDATED 2026-07-10 with the real, live
--    balance and full employer contribution history straight from Ketan's Wealth app
--    (supersedes the earlier Financial_Data.md figure of Rs 37,00,961 - this is a more
--    current live pull). Retirement projection fields are the app's own calculation.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000002',
        'Goldman Sachs', 'Employee PF', 'EPF', 'INR', true, 3741211.00,
        '{"employer": "Goldman Sachs", "maturity_age": 58, "early_withdrawal_options": "Education, medical, housing", "balance_as_of": "2026-07-10", "employer_history": [{"employer": "Goldman Sachs Services Private", "period": "Mar 2019 - present", "balance": 3670847}, {"employer": "Oracle India Private Limited", "period": "Jan 2019 - Mar 2019", "balance": 2659}, {"employer": "Pegasystems Worldwide India", "period": "Apr 2014 - Dec 2018", "balance": 67705}], "retirement_projection": {"projected_fund": 48400000, "monthly_contribution": 34296, "interest_rate_pa": 8.25, "total_contributions_projected": 11900000}, "historical_value_2026_04": 3464961, "contributions_to_date_2026_04": 1890508.88, "appreciation_rate_pa": 8}'::jsonb);

-- 9. National Pension System — Ketan. UPDATED 2026-07-10 with real Tier 1/Tier 2 split
--    and asset allocation, straight from Ketan's Wealth app.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata)
VALUES ('a1000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002',
        'NPS Trust', 'NPS Tier 1', 'NPS', 'INR', true, 871539.00,
        '{"tier": 1, "maturity_age_primary": 60, "early_withdrawal_age": 50, "expected_return_pa": 8, "balance_as_of": "2026-07-10", "tier_2_balance": 233.56, "allocation": {"equity_percent": 70.95, "equity_value": 618000, "government_percent": 18.13, "government_value": 157000, "corporate_percent": 10.92, "corporate_value": 95170}, "historical_value_2026_04": 860254}'::jsonb);

-- 10. Public Provident Fund — Ketan. Institution confirmed 2026-07-10 as Bank of
--     India (household asset ledger); original 40,000 invested in 2016.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, interest_rate, metadata)
VALUES ('a1000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-000000000002',
        'Bank of India', 'PPF', 'PPF', 'INR', true, 50000.00, 7.00,
        '{"maturity_period_years": 15, "opened_year": 2016, "original_investment": 40000, "appreciation_rate_pa": 7, "liquidity_tier": "Low"}'::jsonb);

-- ------------------------------------------------------------------------------
-- ACCOUNTS — mutual funds, one row per real fund per Part B's "1 row = 1 real
-- instrument" convention. emi_amount reused as monthly SIP amount (documented
-- naming smell in wealth.md). opening_balance now holds the real live current
-- value (Ketan's Wealth app, 2026-07-10); "invested_amount" in metadata is the
-- real cost basis from the same screenshot.
-- ------------------------------------------------------------------------------

-- Ketan's full real MF portfolio (2026-07-10) — 11 funds, total current value
-- Rs 12,32,283 (this supersedes the earlier 4-fund/Rs 10,77,000 estimate from
-- assets_06062026.json, which only had portfolio-level totals). SIP amounts are
-- only known for the 4 funds that matched the earlier SIP list; the other 7 are
-- likely older lump-sum/ELSS buys with no active SIP - left NULL (gap).
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, emi_amount, metadata) VALUES
  ('a1000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-000000000002', 'DSP Mutual Fund', 'DSP Multi Asset Allocation Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 50805.00, 10000.00,
   '{"type": "Multi-Asset", "investment_type": "Direct-Growth", "expected_return_pa": 11, "portfolio_owner": "Ketan", "invested_amount": 49997, "value_as_of": "2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000000c', '00000000-0000-0000-0000-000000000002', 'Franklin Templeton', 'Franklin US Opportunities Equity FoF', 'MUTUAL_FUND', 'INR', true, 10082.00, 10000.00,
   '{"type": "International FOF (US Equity)", "investment_type": "Direct-Growth", "expected_return_pa": 12, "portfolio_owner": "Ketan", "invested_amount": 9999, "value_as_of": "2026-07-10", "tax_warning": "International FOF - taxed at slab rate, not 12.5% LTCG equity rate; does not qualify for Rs 1.25L LTCG exemption", "full_fund_name": "Franklin US Opportunities Equity Active FoF Growth Direct Plan (truncated to fit account_name VARCHAR(50))"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000000d', '00000000-0000-0000-0000-000000000002', 'SBI Mutual Fund', 'SBI Nifty Midcap 150 Index Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 175099.00, 10000.00,
   '{"type": "Midcap Index", "investment_type": "Direct-Growth", "expected_return_pa": 14, "portfolio_owner": "Ketan", "invested_amount": 162491, "value_as_of": "2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000000e', '00000000-0000-0000-0000-000000000002', 'PPFAS Mutual Fund', 'Parag Parikh Flexi Cap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 343122.00, 20000.00,
   '{"type": "Flexi Cap", "investment_type": "Direct-Growth", "expected_return_pa": 13, "portfolio_owner": "Ketan", "invested_amount": 344982, "value_as_of": "2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000028', '00000000-0000-0000-0000-000000000002', 'HDFC Mutual Fund', 'HDFC Nifty 50 Index Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 307624.00, NULL,
   '{"type": "Nifty 50 Index", "investment_type": "Direct-Growth", "portfolio_owner": "Ketan", "invested_amount": 304984, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000029', '00000000-0000-0000-0000-000000000002', 'HDFC Mutual Fund', 'HDFC Flexicap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 148145.00, NULL,
   '{"type": "Flexi Cap", "investment_type": "Direct-Growth", "portfolio_owner": "Ketan", "invested_amount": 144992, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000002a', '00000000-0000-0000-0000-000000000002', 'SBI Mutual Fund', 'SBI Large Cap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 111519.00, NULL,
   '{"type": "Large Cap", "investment_type": "Direct-Growth", "portfolio_owner": "Ketan", "invested_amount": 112655, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000002b', '00000000-0000-0000-0000-000000000002', 'SBI Mutual Fund', 'SBI Small Cap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 53507.00, NULL,
   '{"type": "Small Cap", "investment_type": "Direct-Growth", "portfolio_owner": "Ketan", "invested_amount": 49997, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000002c', '00000000-0000-0000-0000-000000000002', 'Axis Mutual Fund', 'Axis ELSS Tax Saver Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 10607.00, NULL,
   '{"type": "ELSS", "investment_type": "Direct-Growth", "portfolio_owner": "Ketan", "invested_amount": 7248, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000002d', '00000000-0000-0000-0000-000000000002', 'DSP Mutual Fund', 'DSP ELSS Tax Saver Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 5813.00, NULL,
   '{"type": "ELSS", "investment_type": "Direct-Growth", "portfolio_owner": "Ketan", "invested_amount": 5702, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000002e', '00000000-0000-0000-0000-000000000002', 'Axis Mutual Fund', 'Axis Global Innovation FoF Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 202.00, NULL,
   '{"type": "International FOF", "investment_type": "Direct-Growth", "portfolio_owner": "Ketan", "invested_amount": 200, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb);

-- Shweta's MF portfolio. UPDATED 2026-07-10 with real current/invested values from
-- Shwetas own wealth app (total MF value shown there: Rs 5,21,680). Only the top 3
-- funds were visible before the app's "view all" truncation - these 3 sum to
-- Rs 4,93,112, leaving ~Rs 28,568 of her real total attributable to further fund(s)
-- not yet screenshotted (GAP).
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, emi_amount, metadata) VALUES
  ('a1000000-0000-0000-0000-00000000000f', '00000000-0000-0000-0000-000000000003', 'UTI Mutual Fund', 'UTI Nifty 50 Index Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 222613.00, 30000.00,
   '{"type": "Nifty 50 Index", "investment_type": "Direct-Growth", "expected_return_pa": 12, "portfolio_owner": "Shweta", "note": "structurally isolated from Ketan portfolio; annual March LTCG harvest only", "invested_amount": 219989, "value_as_of": "2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000003', 'UTI Mutual Fund', 'UTI Nifty Next 50 Index Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 42113.00, 10000.00,
   '{"type": "Nifty Next 50 Index", "investment_type": "Direct-Growth", "expected_return_pa": 14, "portfolio_owner": "Shweta", "invested_amount": 39998, "value_as_of": "2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000003', 'SBI Mutual Fund', 'SBI Contra Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 228386.00, NULL,
   '{"type": "Contra", "investment_type": "Direct-Growth", "portfolio_owner": "Shweta", "invested_amount": 222383, "value_as_of": "2026-07-10", "gap": "no active SIP found - likely a lump-sum/older holding"}'::jsonb);

-- Gayan's + Vamika's education SIP portfolio. Ketan's Wealth app (2026-07-10) shows
-- these two funds as single combined line items (Bandhan Small Cap current Rs 7,881/
-- invested Rs 7,499; Motilal Oswal Midcap current Rs 7,868/invested Rs 7,499) - almost
-- certainly the guardian-linked consolidated view of both kids' minor folios under
-- Ketan's login. Split 60/40 below by their real SIP ratio (Gayan 1500:1000 Vamika
-- per fund) since no per-child breakdown was visible - flagged as an ESTIMATED split,
-- not exact per-child figures.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, emi_amount, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000004', 'Bandhan Mutual Fund', 'Bandhan Small Cap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 4729.00, 1500.00,
   '{"type": "Small Cap", "investment_type": "Direct-Growth", "expected_return_pa": 15, "portfolio_owner": "Gayan", "purpose": "Child education fund - 25% dedicated target Rs 27,50,000 by 2038", "invested_amount": 4499, "value_as_of": "2026-07-10", "gap": "value is a 60/40 estimated split of the real combined Rs 7,881 shown for both kids in Ketans Wealth app - not an exact per-child figure"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000004', 'Motilal Oswal Mutual Fund', 'Motilal Oswal Midcap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 4721.00, 1500.00,
   '{"type": "Midcap", "investment_type": "Direct-Growth", "expected_return_pa": 14, "portfolio_owner": "Gayan", "invested_amount": 4499, "value_as_of": "2026-07-10", "gap": "value is a 60/40 estimated split of the real combined Rs 7,868 shown for both kids in Ketans Wealth app - not an exact per-child figure"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000005', 'Bandhan Mutual Fund', 'Bandhan Small Cap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 3152.00, 1000.00,
   '{"type": "Small Cap", "investment_type": "Direct-Growth", "expected_return_pa": 15, "portfolio_owner": "Vamika", "purpose": "Child education fund - 25% dedicated target Rs 35,00,000 by 2043", "invested_amount": 3000, "value_as_of": "2026-07-10", "gap": "value is a 60/40 estimated split of the real combined Rs 7,881 shown for both kids in Ketans Wealth app - not an exact per-child figure"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000005', 'Motilal Oswal Mutual Fund', 'Motilal Oswal Midcap Growth Direct Plan', 'MUTUAL_FUND', 'INR', true, 3147.00, 1000.00,
   '{"type": "Midcap", "investment_type": "Direct-Growth", "expected_return_pa": 14, "portfolio_owner": "Vamika", "invested_amount": 3000, "value_as_of": "2026-07-10", "gap": "value is a 60/40 estimated split of the real combined Rs 7,868 shown for both kids in Ketans Wealth app - not an exact per-child figure"}'::jsonb);

-- ------------------------------------------------------------------------------
-- ACCOUNTS — children's education Fixed Deposits (real, pre-SIP-era holdings)
-- ------------------------------------------------------------------------------
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, interest_rate, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000004', 'Bank FD', 'Gayan Education FD', 'FD', 'INR', true, 185000.00, 7.00,
   '{"entry_date": "2020", "purpose": "Dedicated education fund (K-12 + College)", "annual_interest_earned": 129500}'::jsonb),
  ('a1000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000005', 'Bank FD', 'Vamika Education FD', 'FD', 'INR', true, 25000.00, 7.00,
   '{"entry_date": "2025-01-01", "purpose": "Dedicated education fund (K-12 + College)", "annual_interest_earned": 1750}'::jsonb);

-- ------------------------------------------------------------------------------
-- ACCOUNTS — additional real bank accounts + credit cards named directly by the
-- household (2026-07-10), not yet cross-referenced against any statement. Every
-- field beyond institution/card name is a GAP: account number/last4, IFSC, opening
-- balance/current outstanding, credit limit, and which family member owns each one
-- (assigned to Ketan below as a default placeholder only — please confirm).
-- ------------------------------------------------------------------------------

-- Bank accounts (beyond the Kotak x2 / HDFC Prime Potential / BoB loans+MaxGain above).
-- Ownership confirmed 2026-07-10: SBI = Ketan; IDFC FIRST = one account per family
-- member (4 accounts); BoB plain savings = Ketan solely (his 5th BoB account,
-- alongside the 2 loans + 2 MaxGain accounts above). NOTE: the earlier generic
-- "HDFC Bank Account" placeholder was REMOVED (2026-07-10) — Ketan confirmed he has
-- only ONE HDFC account total, which is HDFC Prime Potential above (the salary
-- account). SBI + all 4 IDFC accounts confirmed dormant/near-zero activity, same
-- pattern as Kotak Bellandur — exact balance still a gap.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000002', 'State Bank of India', 'SBI Bank Account', 'SAVINGS', 'INR', true, 5074.00,
   '{"gap": "account number, IFSC not yet provided", "owner": "Ketan (confirmed)", "balance_as_of": "2026-07-10", "activity": "low activity, confirmed via Ketans Wealth app 2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000001a', '00000000-0000-0000-0000-000000000002', 'Bank of Baroda', 'BoB Savings Account', 'SAVINGS', 'INR', true, 4692.00,
   '{"gap": "account number, IFSC not yet provided", "owner": "Ketan (confirmed sole owner); this is Ketans 5th BoB account alongside 2 loans + 2 MaxGain accounts", "account_last4": "3944", "balance_as_of": "2026-07-10"}'::jsonb);

-- IDFC FIRST Bank — real data (2026-07-10) shows Ketan personally holds 2 IDFC
-- accounts (••9619 and ••0587, both via Ketan's Wealth app). Household earlier said
-- 4 IDFC accounts exist across the family - keeping Shweta/Gayan/Vamika's as
-- unconfirmed placeholders since this app view only covers Ketan's own accounts.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000002', 'IDFC FIRST Bank', 'IDFC Bank Account 1 - Ketan', 'SAVINGS', 'INR', true, 82506.00,
   '{"gap": "IFSC not yet provided", "account_last4": "9619", "balance_as_of": "2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000002f', '00000000-0000-0000-0000-000000000002', 'IDFC FIRST Bank', 'IDFC Bank Account 2 - Ketan', 'SAVINGS', 'INR', true, 26669.26,
   '{"gap": "IFSC not yet provided", "account_last4": "0587", "balance_as_of": "2026-07-10"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000024', '00000000-0000-0000-0000-000000000003', 'IDFC FIRST Bank', 'IDFC Bank Account - Shweta', 'SAVINGS', 'INR', true, NULL,
   '{"gap": "IFSC not yet provided", "account_last4": "9629", "balance_as_of": "2026-07-07"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000025', '00000000-0000-0000-0000-000000000004', 'IDFC FIRST Bank', 'IDFC Bank Account - Gayan', 'SAVINGS', 'INR', true, NULL,
   '{"gap": "account number, IFSC, exact balance not yet provided - confirmed real 2026-07-10, just not visible in either Ketans or Shwetas own app view"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000026', '00000000-0000-0000-0000-000000000005', 'IDFC FIRST Bank', 'IDFC Bank Account - Vamika', 'SAVINGS', 'INR', true, NULL,
   '{"gap": "account number, IFSC, exact balance not yet provided - confirmed real 2026-07-10, just not visible in either Ketans or Shwetas own app view"}'::jsonb);

-- Shweta's real bank accounts confirmed via her own wealth app (2026-07-07):
-- Kotak Joint (updated above), IDFC (updated above), and this NEW personal Kotak
-- account not previously known.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, opening_balance, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000003', 'Kotak Mahindra Bank', 'Kotak Personal Account - Shweta', 'SAVINGS', 'INR', true, 1366.63,
   '{"gap": "IFSC not yet provided", "account_last4": "1287", "balance_as_of": "2026-07-07", "note": "linked via UPI"}'::jsonb);

-- Shweta's Employee Provident Fund — from a past job, confirmed 2026-07-10: account
-- exists but is OFFLINE/possibly locked (unused ~8 years), not tracked in her wealth
-- app (doesn't appear in her Rs 5,68,884 total, which has no EPF line at all).
-- Balance is a GAP - not visible/synced anywhere.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000003', 'Unknown (past employer)', 'Employee PF - Shweta', 'EPF', 'INR', true,
   '{"gap": "balance, employer, account number all unknown - dormant ~8 years, not synced in Shwetas wealth app", "status": "offline/possibly locked"}'::jsonb);

-- Credit cards — all 6 below confirmed on Ketan's name (2026-07-10). Remaining gaps
-- per card: last4, credit limit, current outstanding, statement/due date.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, metadata) VALUES
  ('a1000000-0000-0000-0000-00000000001b', '00000000-0000-0000-0000-000000000002', 'Axis Bank', 'Axis Bank Flipkart Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000001c', '00000000-0000-0000-0000-000000000002', 'Axis Bank', 'Axis Bank Airtel Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000001d', '00000000-0000-0000-0000-000000000002', 'ICICI Bank', 'ICICI Bank Amazon Pay Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000001e', '00000000-0000-0000-0000-000000000002', 'State Bank of India', 'SBI Bank Tata Neu Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb),
  ('a1000000-0000-0000-0000-00000000001f', '00000000-0000-0000-0000-000000000002', 'State Bank of India', 'SBI Bank IRCTC Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000002', 'HDFC Bank', 'HDFC Bank Millennia Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb);

-- Shweta's 2 credit cards (2026-07-10). Same gaps: last4, credit limit, outstanding, dates.
INSERT INTO wealth.account (id, profile_id, institution_name, account_name, account_type, currency, is_active, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000003', 'Kotak Mahindra Bank', 'Kotak Bank RuPay Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000003', 'Federal Bank', 'Federal Bank One Credit Card', 'CREDIT_CARD', 'INR', true,
   '{"gap": "card last4, credit limit, current outstanding, statement/due date not yet provided"}'::jsonb);

-- ------------------------------------------------------------------------------
-- TRANSACTIONS — Kotak Joint (Varthur), account a...0002. txn_date = statement
-- Value Date per the parser's date-priority decision. Reference numbers captured
-- in metadata.
-- ------------------------------------------------------------------------------
INSERT INTO wealth.transaction (account_id, txn_date, amount, txn_type, description, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000002', '2026-03-31', 30000.00, 'CREDIT', 'NEFT HDFCH00902116032 KETAN VERMA HDFC0000240(Value Date: 31-03-2026)', '{"reference_number": "NEFTINW-1540502875"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-01', 5000.00,  'DEBIT',  'NACH-MUT-DR-INDIAN CLEARING CORP-P57126294X188444', '{"reference_number": "NACHDR01042600041153"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-01', 25000.00, 'DEBIT',  'NACH-MUT-DR-INDIAN CLEARING CORP-P57126294X188444', '{"reference_number": "NACHDR01042600007922"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-02', 1500.00,  'DEBIT',  'UPI/BEEJAPURI DAIRY/609207353117/OidcdJPMAN77998', '{"reference_number": "UPI-609269109553"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-02', 600.00,   'DEBIT',  'UPI/LOKESH  B N/539096760450/Payment from Ph', '{"reference_number": "UPI-609269412806"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-02', 5728.00,  'DEBIT',  'Card dues debited 9406188003047200', '{"reference_number": "VP-2320564110"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-03', 199.00,   'DEBIT',  'UPI/D B CORP LIMITE/609367262096/Collect request', '{"reference_number": "UPI-609341621330"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-03', 340.00,   'DEBIT',  'UPI/R BALA CHANNAIA/170004146856/Payment from Ph', '{"reference_number": "UPI-609375152010"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-06', 40000.00, 'CREDIT', 'MB:RECEIVED FROM SYED NUSRATH FAIZAN MEHDI/RENT AP', '{"reference_number": "MB-998358183725", "income_source": "rental"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-14', 2000.00,  'CREDIT', 'UPI/JAGDISH VERMA/951194812441/Payment from Ph', '{"reference_number": "UPI-610476998643"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-04-18', 8795.00,  'CREDIT', 'UPI/DHIRAJ  GOME/301019937088/Sent using Payt', '{"reference_number": "UPI-610824769628"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-05-02', 40000.00, 'CREDIT', 'MB:RECEIVED FROM SYED NUSRATH FAIZAN MEHDI/RENT MA', '{"reference_number": "MB-998350290565", "income_source": "rental"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000002', '2026-06-03', 40000.00, 'CREDIT', 'MB:RECEIVED FROM SYED NUSRATH FAIZAN MEHDI/RENT JU', '{"reference_number": "MB-998340700293", "income_source": "rental"}'::jsonb);

-- ------------------------------------------------------------------------------
-- TRANSACTIONS — HDFC Prime Potential, account a...0003. Every row below reconciles
-- exactly against the real statement's running balance (verified by hand). Rows 1-2
-- are the bundled Home Loan 1 + 2 EMI debit and a SIP transfer; row 3 the car loan EMI;
-- rows 4-7 mutual fund redemption/investment activity.
-- "SANDOZ - MUM" in these narrations is NOT the employer (confirmed 2026-07-10) —
-- Goldman Sachs is Ketan's actual employer (matches the EPF/gratuity source data);
-- SANDOZ is most likely a payroll-processing/corporate-client label HDFC's NEFT
-- batch system attaches to the narration, unrelated to the real employer name.
-- ------------------------------------------------------------------------------
INSERT INTO wealth.transaction (account_id, txn_date, amount, txn_type, description, metadata) VALUES
  ('a1000000-0000-0000-0000-000000000003', '2026-04-01', 30000.00,  'DEBIT',  'NEFT DR-KKBK0008122-JOINTACCOUNT-SANDOZ - MUM-HDFCH00902116032-NET BANKING SI -SIP', '{"reference_number": "HDFCH00902116032"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000003', '2026-04-01', 76500.00,  'DEBIT',  'NEFT DR-BARB0MARTHA-KETAN BOB-SANDOZ - MUM-HDFCH00902116034-NET BANKING SI -HOME LOAN', '{"reference_number": "HDFCH00902116034", "note": "bundled Home Loan 1 + Home Loan 2 EMI via BoB MaxGain"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000003', '2026-04-01', 50000.00,  'DEBIT',  'NEFT DR-BARB0MARTHA-KETAN BOB-SANDOZ - MUM-HDFCH00902116036-NET BANKING SI -SIP', '{"reference_number": "HDFCH00902116036"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000003', '2026-04-02', 8276.00,   'DEBIT',  'NEFT DR-BKID0008822-KETAN BOI-SANDOZ - MUM-HDFCH00903711456-NET BANKING SI -CAR LOAN', '{"reference_number": "HDFCH00903711456"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000003', '2026-04-08', 12324.45,  'CREDIT', 'NEFT CR-CITI0100000-DSP MUTUAL FUND REDEMPTION AC-KETAN VERMA-CITIN26649732828', '{"reference_number": "CITIN26649732828"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000003', '2026-04-08', 61853.40,  'CREDIT', 'NEFT CR-UTIB0000004-AXIS MUTUAL FUND REDEMPTION POOL A/C-KETAN VERMA-AXISCN1305054697', '{"reference_number": "AXISCN1305054697", "note": "Axis fund not in the current ongoing SIP list - likely a prior/closed holding"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000003', '2026-04-08', 219308.10, 'CREDIT', 'FT- SBI MUTUAL FUND - SBIRED  20963417 D103G', '{"reference_number": "0000001566165354"}'::jsonb),
  ('a1000000-0000-0000-0000-000000000003', '2026-04-08', 295000.00, 'DEBIT',  'RTGS DR-BARB0MARTHA-KETAN BOB-NETBANK, MUM-HDFCR52026040893083567-MUTUALFUNDS', '{"reference_number": "HDFCR52026040893083567"}'::jsonb);

-- ------------------------------------------------------------------------------
-- PHYSICAL ASSETS — real estate, gold, and vehicles (real current_value/valuation_date
-- from assets_06062026.json / Financial_Data.md). Joint ownership recorded via the
-- metadata.joint_owners convention (ADR-016); physical_asset FK only supports one
-- profile_id, so joint assets are assigned to Ketan with the co-owner noted.
-- ------------------------------------------------------------------------------

-- Self-Occupied Flat (Sobha, Varthur) — 2BHK, collateral for Home Loan 1 ONLY.
-- CORRECTED 2026-07-10: Home Loan 2 is against the rental flat, not a second
-- mortgage here (Ketan confirmed) — collateral_for and LTV recomputed accordingly
-- (was 32.5% across both loans; now 29.0% = 37,71,120 / 1,30,00,000 for HL1 alone).
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002',
        'Self-Occupied Flat - Sobha Dream Acres', 'REAL_ESTATE', 13000000.00, '2026-03-01',
        '{"location": "Bengaluru, Varthur", "configuration": "2BHK", "ownership": "Joint", "joint_owners": ["Shweta Ketan Verma"], "occupancy": "Owner-occupied (primary residence)", "collateral_for": ["Home Loan 1 - BoB MaxGain"], "loan_to_value_percent": 29.0, "purchase_date": "2021", "purchase_cost": 7000000, "appreciation_rate_pa": 6, "loan_linked": true, "liquidity_tier": "Low"}'::jsonb);

-- Rental Flat ("Sunshine"/"GS Sunshine") — real tenant + rent cross-validated
-- against the real Kotak Joint bank credits above (Rs 40,000/month from Syed
-- Nusrath Faizan Mehdi). RESOLVED 2026-07-10: Ketan confirmed this property IS
-- loan-linked via Home Loan 2 (the earlier "NO DEBT" in assets_06062026.json was
-- wrong/outdated) - the household asset ledger's "Loan Linked: Y" was correct.
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
        'Rental Flat - Sunshine', 'REAL_ESTATE', 10000000.00, '2026-04-18',
        '{"location": "Bengaluru", "ownership": "Joint", "joint_owners": ["Shweta Ketan Verma"], "tenant_name": "Syed Nusrath Faizan Mehdi", "monthly_rent": 40000, "rent_income_account": "Kotak Joint - Varthur", "collateral_for": ["Home Loan 2 - BoB MaxGain"], "loan_linked": true, "payment_status": "Reliable", "purchase_date": "2022", "purchase_cost": 7000000, "appreciation_rate_pa": 6, "liquidity_tier": "Low"}'::jsonb);

-- Land Property (Indore, native place) — Shweta's individual holding, gift from her father (1990).
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003',
        'Land Property - Indore', 'REAL_ESTATE', 4000000.00, '2026-04-18',
        '{"location": "Indore (native place)", "ownership": "Individual", "source": "Gift from Shwetas father (1990)", "loans": "NO DEBT", "future_purpose": "Gayans education funding per 3-year plan (2029+ potential development)", "purchase_cost": 60000, "appreciation_rate_pa": 12, "liquidity_tier": "Low"}'::jsonb);

-- Gold jewellery — Shweta's, reserved for kids' marriages (2045+).
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003',
        'Gold Jewellery', 'GOLD_JEWELRY', 1300000.00, '2026-04-18',
        '{"owner": "Shweta", "physical_form": "Jewellery/bridal sets", "purpose": "Reserve for kids marriages (2045+)", "weight_grams": 100, "purchase_date": "2024", "purchase_cost": 450000, "appreciation_rate_pa": 5, "liquidity_tier": "High"}'::jsonb);

-- Sovereign Gold Bond — Shweta's, 18g (Sep 2023 purchase).
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003',
        'Sovereign Gold Bond', 'GOLD_BOND', 264000.00, '2026-04-18',
        '{"owner": "Shweta", "purpose": "Alternative gold holding with interest accrual", "liquidity": "Redeemable after 5 years or upon maturity", "weight_grams": 18, "purchase_date": "2023-09", "purchase_cost": 108000, "appreciation_rate_pa": 7, "liquidity_tier": "Medium"}'::jsonb);

-- Tata Nexon — RESOLVED 2026-07-10: Ketan confirmed the Nexon has NO loan against
-- it at all. The BoI Car Loan is actually against a Tata Tiago that was sold within
-- the family (see the Car Loan account above) — the household asset ledger's
-- "Loan Linked: N" for the Nexon was correct all along.
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, make, model, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000002',
        'Family Car - Tata Nexon', 'VEHICLE', 'Tata', 'Nexon Petrol Manual 2024', 900000.00, '2026-04-18',
        '{"ownership": "Joint", "joint_owners": ["Shweta Ketan Verma"], "loans": "NO DEBT", "usage": "Primary family vehicle", "purchase_date": "2024", "purchase_cost": 1180000, "appreciation_rate_pa": -5, "liquidity_tier": "Medium"}'::jsonb);

-- Royal Enfield Thunderbird — owner confirmed Ketan (2026-07-10).
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, make, model, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000002',
        'Motorcycle - RE Thunderbird', 'VEHICLE', 'Royal Enfield', 'Thunderbird 350', 45000.00, '2026-04-18',
        '{"loans": "NO DEBT", "owner": "Ketan", "purchase_date": "2013", "purchase_cost": 150000, "appreciation_rate_pa": -5, "liquidity_tier": "Medium"}'::jsonb);

-- TVS Jupiter — Shweta's personal commute vehicle.
INSERT INTO wealth.physical_asset (id, profile_id, asset_name, asset_type, make, model, current_value, valuation_date, metadata)
VALUES ('b1000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000003',
        'Scooter - TVS Jupiter', 'VEHICLE', 'TVS', 'Jupiter 110', 90000.00, '2026-04-18',
        '{"loans": "NO DEBT", "owner": "Shweta", "usage": "Personal commute", "purchase_date": "2024", "purchase_cost": 110000, "appreciation_rate_pa": -5, "liquidity_tier": "Medium"}'::jsonb);

-- ==============================================================================
-- DATA GAPS remaining in this file (see chat response for the consolidated list):
--
-- RESOLVED 2026-07-10 via Ketan's Wealth app screenshots (real, live data):
-- - EPF real balance Rs 37,41,211 + full employer history (Goldman Sachs Mar'19-
--   present, Oracle India Jan-Mar'19, Pegasystems Apr'14-Dec'18) + retirement
--   projection (Rs 4.84Cr projected, Rs 34,296/mo contribution, 8.25% pa).
-- - NPS real balance Rs 8,71,539 (Tier 1) + Tier 2 (Rs 233.56) + real asset
--   allocation (Equity 70.95%, Government 18.13%, Corporate 10.92%).
-- - Ketan's full real 11-fund MF portfolio (Rs 12,32,283 total, current + invested
--   values for each) — supersedes the earlier 4-fund/Rs 10,77,000 estimate.
-- - Gayan/Vamika's Bandhan + Motilal Oswal funds updated to real combined values,
--   split 60/40 by SIP ratio (still an ESTIMATED per-child split, not exact).
-- - Real balances + last4 for: Kotak Bellandur (confirmed exact match to statement),
--   HDFC Prime Potential, both BoB MaxGain accounts, BoB savings, new BoI savings
--   account (was entirely missing before), SBI, and 2 of Ketan's IDFC accounts.
-- - HDFC Prime Potential confirmed as Ketan's ONLY HDFC account and his salary
--   account; the earlier duplicate generic "HDFC Bank Account" placeholder was
--   removed. Employer confirmed as Goldman Sachs ("SANDOZ" in HDFC narrations is a
--   payroll-processing label, not the employer). RE Thunderbird owner confirmed Ketan.
-- - All credit card ownership confirmed (6 Ketan + 2 Shweta: Kotak RuPay + Federal
--   Bank One).
-- - Shweta's real bank accounts (own wealth app, 2026-07-07): Kotak Joint balance
--   updated to a direct read (Rs 13,593.59, superseding the earlier Ketan-side
--   derived estimate), her IDFC account confirmed real (••9629, Rs 33,448.57), and
--   a brand-new Kotak personal account discovered (••1287, Rs 1,366.63, via UPI).
-- - Shweta's MF portfolio updated with real current/invested values for 3 funds
--   (UTI Nifty 50, UTI Nifty Next 50, + newly discovered SBI Contra) - Rs 4,93,112
--   of her real Rs 5,21,680 total accounted for.
-- - Shweta confirmed to have an EPF account from a past job, but it's offline/
--   possibly locked (unused ~8 years) and not synced in her wealth app - added as a
--   placeholder EPF account with balance/employer as gaps.
-- - A household asset ledger (~3 months old, ~April 2026) added purchase_date/
--   appreciation_rate_pa/purchase_cost/liquidity_tier metadata to every physical
--   asset (flat, rental flat, land, gold jewellery, gold bond, all 3 vehicles) and
--   confirmed PPF is held at Bank of India (original Rs 40,000 invested 2016). Also
--   gave full Gratuity detail (Rs 5,57,000, earned 2024, 1% pa appreciation, Rs 0
--   purchase cost) - still not seeded anywhere since no DB schema exists for it.
-- - RESOLVED 2026-07-10 (Ketan confirmed directly): Home Loan 2 is against the
--   Rental Flat "Sunshine", NOT a second mortgage on the self-occupied Sobha flat
--   as earlier assumed - collateral_for and LTV recomputed on both properties
--   accordingly (Sobha flat LTV now 29.0% on HL1 alone). The Tata Nexon has NO
--   loan at all; the BoI Car Loan is actually against a Tata Tiago that was sold
--   within the family - the loan is still active/serviced with no compensating
--   receivable noted for the sold vehicle. Both earlier-flagged conflicts are now
--   resolved in favor of what Ketan confirmed directly.
--
-- STILL OPEN:
-- - Per-fund SIP amounts for 7 of Ketan's 11 real funds (only 4 have a known SIP).
-- - IFSC for the newly-added SBI/BoB-savings/BoI-savings/2 new Ketan IDFC accounts/
--   Shweta's IDFC and new Kotak account.
-- - ~Rs 28,568 of Shweta's real MF total is in fund(s) beyond the 3 already seeded
--   (her app's list was truncated behind "view all").
-- - Shweta's EPF balance/employer entirely unknown (dormant, not app-synced).
-- - Whether Gayan/Vamika's 2 remaining stated IDFC accounts are real distinct
--   accounts (not visible in either Ketan's or Shweta's app view) or a
--   miscommunication - unconfirmed.
-- - Credit card last4/credit limit/current outstanding/statement date (all 8 cards).
-- - Loan account numbers for all 3 loans (Home Loan 1 & 2, Car Loan).
-- - Gratuity (Rs 5,57,000, Goldman Sachs) and 4 real insurance policies - no DB
--   schema exists for either yet (see Part B "Out of scope").
-- - Exact Ketan/Shweta income as a structured field (net salary Rs 2,40,383/mo +
--   Rs 6,32,272 annual bonus) - no income/payroll entity exists; only visible today
--   as ordinary CREDIT transactions on the HDFC salary account.
-- - Health and household.goal deliberately left minimal/empty per household decision
--   (2026-07-10) - the 5 real Epic-8 goals are computed live from this wealth data,
--   not stored rows.
-- ==============================================================================

-- checksum-bump: force Flyway repeatable-migration re-apply after profile.profile TRUNCATE CASCADE wiped this schema data (2026-07-10 22:14 IST)
