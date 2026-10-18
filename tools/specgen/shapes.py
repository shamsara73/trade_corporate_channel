"""Message bodies that many TF* messages share. Each entry is
(canonical message, title, {values, ctx, notes}); the sample values are
illustrative."""

SENDER = {'Sender/Customer': 'ACMEIMP'}

SHAPES = [
    ('TFISBCOR', 'Correspondence', {
        'ctx': {'Product': 'ISB'},
        'values': dict(MasterRef='ISB2600045', **SENDER, **{
            'Subject': 'Query on amendment 2', 'CorresType': 'Q',
            'RelatedRef': 'AMD-2', 'MessageText': 'Please confirm whether the new expiry date was advised to the beneficiary.',
            'eBankMasterRef': 'CH-ISB-000045', 'eBankEventRef': 'CH-ISB-000045-07'}),
        'notes': ('Used for every `*COR` message and, with small additions, for the basic text amendment requests (`TFISBAMR`, `TFIGTAMR`, '
                  '`TFCOLAMR`) and `TFCOLFNR`. `CorresType` is G general, Q query, A answer, N ancillary. '
                  '`EventFurtherID` must be a bank-defined dictionary value (`EVTxx`). `FormOfUndertaking` and `Function` apply to SWIFT MT759-type messages. '
                  'To answer a bank query, set `CorresType` = `A` and `RelatedRef` to the bank\'s reference.'),
    }),
    ('TFISBCAN', 'Cancellation request', {
        'ctx': {'Product': 'ISB'},
        'values': dict(MasterRef='ISB2600045', **SENDER, **{
            'CancelInstructions': 'Underlying contract completed. Please cancel the standby.',
            'BenApprovalReq': 'N', 'eBankMasterRef': 'CH-ISB-000045', 'eBankEventRef': 'CH-ISB-000045-09'}),
        'notes': '`BenApprovalReq` states whether the beneficiary\'s consent is required. Whether the bank can cancel without it depends on the instrument and the governing rules.',
    }),
    ('TFISBPYR', 'Claim response', {
        'ctx': {'Product': 'ISB'},
        'values': dict(MasterRef='ISB2600045', **SENDER, **{
            'ClaimId': 'CLM001', 'ResponseAction': None, 'ResponseDetails': 'Documents are in order. Approve payment.',
            'NetAmountPaid/Amount': '250000.00', 'NetAmountPaid/Currency': 'USD',
            'AuthorisationInstructions': 'PAY', 'eBankMasterRef': 'CH-ISB-000045', 'eBankEventRef': 'CH-ISB-000045-10'}),
        'notes': ('The applicant approves or refuses a claim or presentation that the bank registered on the master. '
                  '`ClaimId` identifies the claim. Give `ResponseAction` **or** `ResponseType`; the allowed codes are the bank\'s payment actions in TI system tailoring and must be loaded into the channel. '
                  '`AuthorisationInstructions` must be **upper case** because it feeds SWIFT MT752 field 23. '
                  '`RequestedNewExpiryDate` supports an "extend or pay" demand. For export-side members (`TFEGTPYR`, `TFESBPYR`, `TFUTRPYR`) the same body is a beneficiary-side message and is not a customer instruction.'),
    }),
    ('TFELCBMR', 'Beneficiary response', {
        'ctx': {'Product': 'ELC'},
        'values': dict(MasterRef='ELC2600012', **SENDER, **{
            'AmendRef': 'AMD-1', 'ResponseType': 'A', 'ResponseDetails': 'Beneficiary accepts the amendment.',
            'eBankMasterRef': 'CH-ELC-000012', 'eBankEventRef': 'CH-ELC-000012-03'}),
        'notes': '`ResponseType` is `A` approved or `R` refused. `TFxxxBMR` answers an amendment (`AmendRef`); `TFxxxBCR` answers a cancellation (`CancelRef`). Members that omit `AmendRef` carry `CancelRef` instead.',
    }),
    ('TFESBDPR', 'Documents presented', {
        'ctx': {'Product': 'ESB'},
        'values': dict(MasterRef='ESB2600031', **SENDER, **{
            'PresentersAction': None, 'NotesFromPresenter': 'Documents presented under cover of letter dated 2026-10-15.',
            'PresentationDetails/PresentationAmount/Amount': '120000.00',
            'PresentationDetails/PresentationAmount/Currency': 'USD',
            'PresentationDetails/PresentationDate': '2026-10-15', 'PresentationDetails/PresentersReference': 'PRES-8812',
            'eBankMasterRef': 'CH-ESB-000031', 'eBankEventRef': 'CH-ESB-000031-04'}),
        'notes': 'Beneficiary-side presentation of documents or a demand. `PresentersAction` / `RequestType` use the bank\'s payment-action tailoring, as for claim responses.',
    }),
    ('TFIOARBC', 'Open account response', {
        'ctx': {'Product': 'IOA'},
        'values': dict(MasterRef='IOA2600007', **SENDER, **{
            'PurchaseOrderReference': 'PO-2026-0457', 'TMAUsed': 'Y', 'TransactionMatchingReference': 'TMA-99817',
            'ResponseType': 'A', 'ResponseNarrative': 'Cancellation request accepted.', 'ResponseDate': '2026-10-18',
            'eBankMasterRef': 'CH-IOA-000007', 'eBankEventRef': 'CH-IOA-000007-02'}),
        'notes': ('Answers an amendment or cancellation request on an open-account (TMA) transaction. `ResponseType` is `A` accepted or `R` rejected. '
                  'Members are named for who answers whom: buyer, seller, or the bank\'s transaction-matching application (TMA).'),
    }),
    ('TFIOABRC', 'Open account cancellation request', {
        'ctx': {'Product': 'IOA'},
        'values': dict(MasterRef='IOA2600007', **SENDER, **{
            'PurchaseOrderReference': 'PO-2026-0457', 'TMAUsed': 'Y', 'TransactionMatchingReference': 'TMA-99817',
            'CancellationRef': 'CXL-1', 'CancellationDate': '2026-10-18',
            'CancellationInstructions': 'Order cancelled by mutual agreement.',
            'InformationForBuyer': 'Please confirm cancellation.',
            'eBankMasterRef': 'CH-IOA-000007', 'eBankEventRef': 'CH-IOA-000007-03'}),
    }),
    ('TFIOAAME', 'Open account amendment', {
        'ctx': {'Product': 'IOA'},
        'values': dict(MasterRef='IOA2600007', **{
            'PurchaseOrderReference': 'PO-2026-0457', 'TMAUsed': 'Y', 'TransactionMatchingReference': 'TMA-99817',
            'AmendmentDate': '2026-10-18', 'ExpiryDate': '2027-02-28', 'AmendmentNarrative': 'Extend the latest payment date by 30 days.',
            'OpenAccountAmount/Amount': '180000.00', 'OpenAccountAmount/Currency': 'USD',
            'eBankMasterRef': 'CH-IOA-000007', 'eBankEventRef': 'CH-IOA-000007-04'}),
    }),
    ('TFIRFRPY', 'Finance repayment', {
        'ctx': {'Product': 'FIN'},
        'values': dict(MasterRef='FIN2600101', **{
            'EBankMasterRef': 'CH-FIN-000101', 'EBankEventRef': 'CH-FIN-000101-02',
            'RepaymentAction': None, 'RepaymentReference': 'RPY-2026-10-18', 'ValueDate': '2026-10-20',
            'RepaymentPayer': 'D', 'PrincipalRepaymentAmount/Amount': '500000.00', 'PrincipalRepaymentAmount/Currency': 'USD',
            'InterestRepaymentAmount/Amount': '4200.00', 'InterestRepaymentAmount/Currency': 'USD',
            'TotalRepaymentAmount/Amount': '504200.00', 'TotalRepaymentAmount/Currency': 'USD',
            'PrincipalAccount': '1234567890', 'CustomerInstructions': 'Debit the operating account.'}),
        'notes': ('One body serves every repayment, whatever the underlying finance (import/export LC, collection, open account, invoice, SCF, standalone). '
                  '`RepaymentAction` codes include F final payment and I interest-only; see the enumeration table. `RepaymentPayer` is D debit party or F finance party.'),
    }),
    ('TFIOAPBR', 'Open account payment-request response', {
        'ctx': {'Product': 'IOA'},
        'values': dict(MasterRef='IOA2600007', **{
            'EBankMasterRef': 'CH-IOA-000007', 'EBankEventRef': 'CH-IOA-000007-05',
            'InvoiceReference': 'INV-77120', 'PurchaseOrderReference': 'PO-2026-0457', 'TransactionMatchingReference': 'TMA-99817',
            'RequestResponse/RequestResponseAction': None, 'RequestResponse/Discrepancies': 'Quantity differs from purchase order.',
            'Instructions': 'Accept with the stated discrepancy.', 'ResponsePartyReference': 'BUY-0042'}),
    }),
    ('TFELCAOP', 'Assignment of proceeds / transfer', {
        'ctx': {'Product': 'ELC'},
        'values': dict(MasterRef='ELC2600012', **SENDER, **{
            'Amount/Amount': '100000.00', 'Amount/Currency': 'USD', 'Instructions': 'Assign 100,000.00 of proceeds to the second beneficiary.',
            'eBankMasterRef': 'CH-ELC-000012', 'eBankEventRef': 'CH-ELC-000012-05'}),
    }),
    ('TFUTRBCR', 'Undertaking beneficiary response', {
        'ctx': {'Product': 'UTR'},
        'values': dict(MasterRef='UTR2600003', **SENDER, **{
            'OriginalEventReference': 'AMD-1', 'ResponseDate': '2026-10-18', 'BeneficiaryResponseDetails': 'Beneficiary accepts.',
            'eBankMasterRef': 'CH-UTR-000003', 'eBankEventRef': 'CH-UTR-000003-02'}),
    }),
    ('TFCOLCON', 'Collection payment', {
        'ctx': {'Product': 'IDC'},
        'values': dict(MasterRef='IDC2600020', **SENDER, **{
            'Subject': 'Payment instruction', 'CorresType': 'G',
            'CollectionAmount/Amount': '75000.00', 'CollectionAmount/Currency': 'USD', 'CollectionDate': '2026-10-20',
            'PaymentAction': None, 'eBankMasterRef': 'CH-IDC-000020', 'eBankEventRef': 'CH-IDC-000020-03'}),
        'notes': ('Drawee payment or "continue payment" instruction under a collection. `Financing` lets the payment come with a financing request. '
                  '`PaymentAction` is a bank-tailored code. `PaymentDraftss` lists the drafts being paid.'),
    }),
    ('TFBUYFIN', 'Invoice finance request', {
        'ctx': {'Product': 'INV'},
        'values': dict(MasterRef='SCF2600008', **{
            'TheirRef': 'FIN-REQ-1', 'eBankMasterRef': 'CH-INV-000008', 'eBankEventRef': 'CH-INV-000008-02',
            'Programme': 'PROG01', 'FinanceCurrency': 'USD', 'FinancePercent': '90.00', 'FinanceDate': '2026-10-20', 'MaturityDate': '2027-01-20'}),
    }),
    ('TFINVAMD', 'Invoice create or amend', {
        'ctx': {'Product': 'INV'},
        'values': dict(MasterRef='INV2600155', **{
            'TheirRef': 'INV-77120', 'eBankMasterRef': 'CH-INV-000155', 'eBankEventRef': 'CH-INV-000155-01',
            'Programme': 'PROG01', 'InvoiceNumber': 'INV-77120', 'IssueDate': '2026-10-10',
            'FaceValue/Amount': '60000.00', 'FaceValue/Currency': 'USD', 'SettlementDate': '2027-01-10'}),
    }),
    ('TFCPCCRT', 'Customer payment', {
        'ctx': {'Product': 'CPC'},
        'values': {'OrderingParty/Customer': 'ACMEIMP', 'Remitter/Customer': 'ACMEIMP', 'RemitterAccount': '1234567890',
                   'RemitterAmount/Amount': '15000.00', 'RemitterAmount/Currency': 'USD',
                   'BeneficiaryAmount/Currency': 'USD', 'PaymentDate': '2026-10-20',
                   'Beneficiary/NameAddress': 'SHENZHEN WIDGETS CO LTD\n88 NANSHAN AVENUE\nSHENZHEN', 'Beneficiary/Country': 'CN',
                   'BenAccountNo': '6222000011112222', 'BeneficiaryBank/SwiftAddress': 'ICBKCNBJXXX',
                   'OurChargesFor': None, 'BenChargesFor': None,
                   'eBankMasterRef': 'CH-CPC-000301', 'eBankEventRef': 'CH-CPC-000301-01'},
    }),
    ('TFIOARPY', 'Open account payment presentation', {
        'ctx': {'Product': 'IOA'},
        'values': dict(MasterRef='IOA2600007', **{
            'EBankMasterRef': 'CH-IOA-000007', 'EBankEventRef': 'CH-IOA-000007-06',
            'InvoiceReference': 'INV-77120', 'PurchaseOrderReference': 'PO-2026-0457', 'TransactionMatchingReference': 'TMA-99817',
            'InvoiceIssueDate': '2026-10-10'}),
    }),
    ('TFEOACPR', 'Export open account payment response', {
        'ctx': {'Product': 'EOA'},
        'values': dict(MasterRef='EOA2600011', **{
            'EBankMasterRef': 'CH-EOA-000011', 'EBankEventRef': 'CH-EOA-000011-02',
            'InvoiceReference': 'INV-88001', 'PurchaseOrderReference': 'PO-9981', 'TransactionMatchingReference': 'TMA-10442',
            'ResponseDetails': 'Payment received from buyer.'}),
    }),
    ('TFFINNEW', 'Finance create', {
        'ctx': {'Product': 'FIN'},
        'values': {'FinanceAmount/Amount': '800000.00', 'FinanceAmount/Currency': 'USD', 'FinanceCurrency': 'USD',
                   'StartDate': '2026-10-20', 'MaturityDate': '2027-01-18', 'AutoRepay': 'Y',
                   'DebitParty/Customer': 'ACMEIMP', 'FinanceToParty/Customer': 'ACMEIMP',
                   'PrincipalAccount': '1234567890', 'ChargeAccount': '1234567890',
                   'MessageToBank': 'Finance against import LC ILC2600123.', 'RelatedMasterRef': 'ILC2600123',
                   'eBankMasterRef': 'CH-FIN-000101', 'eBankEventRef': 'CH-FIN-000101-01'},
    }),
    ('TFIBPAMD', 'Bulk payment', {
        'ctx': {'Product': 'IBP'},
        'values': {'BulkPaymentBankDiscretion': 'N', 'SettlementAmount/Amount': '420000.00', 'SettlementAmount/Currency': 'USD',
                   'SettlementDate': '2027-01-10', 'PreferredCurrency': 'USD', 'EBankEventRef': 'CH-IBP-000009-01'},
    }),
]
