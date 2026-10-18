"""Product definitions: import side (customer is applicant, buyer or drawee)."""
from defs_common import M, SCHEMA_RULES_GENERIC, HIDDEN_COMMON, CHANNEL_MANAGED_REFS

ILC_DOC = '[Import LC specification](import-letter-of-credit.md)'

# --------------------------------------------------------------------------- ISB
ISB = dict(
    id='ISB', file='import-standby-lc.md', title='Import Standby Letter of Credit',
    priority='P1, wave 2 (after Import LC)', role='Applicant', product='ISB',
    intro=("The customer asks the bank to issue a **standby letter of credit** in favour of a beneficiary. A standby is a bank undertaking that pays on "
           "presentation of a demand or statement, typically to secure a payment or performance obligation (financial standby) or a trade obligation (trade standby). "
           "It is governed by ISP98, UCP 600 or URDG 758, and unlike a documentary LC it is usually drawn only if the customer defaults. "
           "In TI the standby shares the Import LC data model, so this specification is written as a delta to the "
           + ILC_DOC + "; the standby-specific elements are `Trade`, `Financial`, drawing options, renewal, expiry details, delivery and governing law."),
    out_of_scope=("data take-on of existing standbys, SWIFT message generation, and the back-to-back variant (not offered for standbys in the interface)."),
    messages=[
        M('TFISBAPP', 'Issue standby', 'Create / issue', phase='2', baseline='TFILCAPP', baseline_label='TFILCAPP',
          baseline_where='[Import LC specification, section 5.4](import-letter-of-credit.md)'),
        M('TFISBAMN', 'Amend standby', 'Amend', baseline='TFISBAPP', baseline_label='TFISBAPP', baseline_where='section 6.3 of this document'),
        M('TFISBAMR', 'Basic amendment request (free text)', 'Amend', note='Use when the customer wants to describe a change in words and let the TSO key it. Same body as correspondence plus `AmendmentNarrative`.'),
        M('TFISBCAN', 'Cancel standby', 'Cancel'),
        M('TFISBPYR', 'Respond to a demand', 'Claim response', note='A demand under the standby is registered by the bank as a claim. The applicant approves or refuses it.'),
        M('TFISBCOR', 'Correspondence', 'Correspondence'),
    ],
    primary='TFISBAPP',
    scenario=("A construction company must give its client a performance security. It asks the bank to issue a standby for USD 1,000,000, valid for 12 months, "
              "payable on the beneficiary's signed statement that the contractor is in default, with automatic annual renewal unless the bank gives notice. "
              "The TSO checks the facility, applies standard wording, and issues the standby by SWIFT (MT760). If the beneficiary later makes a demand, TI "
              "registers a claim, the channel alerts the customer, and the customer approves or refuses payment within the examination period."),
    flows=[dict(kind='issue', title='Issuance', what='standby application', msg='TFISBAPP', event='Import Standby LC issue event'),
           dict(kind='respond', title='Demand received', what='demand response', msg='TFISBPYR', event='Import Standby LC outstanding claim event',
                trigger='Notification: claim registered on the standby')],
    pre=["The corporate is enabled for product `ISB` and the user has *create* or *approve* entitlement within their amount limit.",
         "The bank has a credit facility for standbys for this customer. TI checks it; the channel does not.",
         "Reference data is loaded: currencies, countries, standby `ProductType` values, applicable-rules codes, delivery methods.",
         "For a demand response, the channel has received (or polls for) the claim and knows its `ClaimId`."],
    screens_intro="The wizard follows the Import LC form; sections that differ are marked.",
    screens=[
        ('Basics', 'Applicant `Applicant` (pre-filled); your reference `Context.TheirReference`; application date `ApplicationDate`; template `Context.TemplateIdentifier`'),
        ('Beneficiary', '`Beneficiary`; `BenAccNo`; advising bank `AdvisingBank`; advise through `AdviseThru`; confirming bank `ConfirmingBank` (**standby-specific**)'),
        ('Purpose', 'Financial or trade standby `Financial` / `Trade` (**standby-specific**); standby flag `StandBy`; product type `ProductType`, `OtherProductType`'),
        ('Amount', '`LCAmount`; tolerance `LCAmountSpec`; additional amounts `AddAmount`, `AddAmountText`'),
        ('Validity', 'Expiry `ExpiryDetails` (type, date, condition); expiry place `ExpiryPlace`; "not payable before" `NotPayableBeforeDate`; renewal `Renewal`, `RenewalDetails`; domestic expiry `DomesticExpiry`'),
        ('Drawing', 'Partial drawings `PartialDrawingsAllowed`; multiple drawings `MultipleDrawingsAllowed`; transferable `Transferable`, `TransferableConditions`; documents required for a demand `Documents`; additional conditions `AdditionalConditions`'),
        ('Rules and law', 'Applicable rules `ApplicableRules` (rules and narrative); governing law and jurisdiction `GoverningLawDetails`; free-format SWIFT wording `UseFreeFormat`'),
        ('Delivery', 'How the original is delivered `UndertakingDeliveryDetails`'),
        ('Charges and accounts', '`IssuanceChgsFor`, `OverseasChgsFor`, `TaxFor`, `DeferChgs`, `ChargeAccount`, `PreferredCcy`, `PrincipalAccount`, reimbursing bank'),
        ('Cover and risk', '`MarginDeposit`, `Collateral`, `FxContracts`, `FXOptDealRef`'),
        ('Instructions and documents', '`ApplicantInstructions`, `InstructionsToAdvisingBank`, `InstructionsToPayingBank`, attachments'),
        ('Review and submit', 'Summary, preview, approval route')],
    hidden=HIDDEN_COMMON + " Also `ApplicantBank`, `InstructionsToApplicant`, `PreAdvise`.",
    master_note='Empty for a new standby; TI allocates the reference. For all later messages set `MasterRef` to the standby reference.',
    channel_managed=CHANNEL_MANAGED_REFS,
    schema_rules=(SCHEMA_RULES_GENERIC +
                  "Points that are easy to get wrong:\n\n"
                  "* `ExpiryDetails.ExpiryType` is **`C` Conditional, `N` Specific date, `Y` Unlimited**. `Y` does not mean \"has an expiry\".\n"
                  "* `ApplicableRules` is a group (`Rules`, `Narrative`) in standbys, whereas the Import LC uses the single element `ApplcableRules` (sic). "
                  "`Rules` ∈ {`ISPR`, `UCPR`, `URDG`, `NONE`, `OTHR`}; `Narrative` (≤ 35) only for `OTHR`.\n"
                  "* `UndertakingDeliveryDetails.DeliveryByMethod` ∈ {`COLL`, `COUR`, `MAIL`, `MESS`, `OTHR`, `REGM`}.\n"
                  "* `Renewal.RenewalWhen` ∈ {`C` on calendar, `E` on expiry}; `Renewal.UseAmount` ∈ {`C` current, `O` original}."),
    rules=["Exactly one expiry mode: `ExpiryType` = `N` requires `ExpiryDetails.Date`; `C` requires `Condition`; `Y` requires neither. A date expiry is after today.",
           "If `Renewal` is used, `NextExpiryDate` or `RenewForPeriod` is present and `NextExpiryDate` is after `ExpiryDetails.Date`; a rolling renewal needs `NumberOf` or a final expiry date.",
           "`NotPayableBeforeDate` is not after the expiry date.",
           "`Financial` and `Trade` are not both `Y`. **[Proposed]**",
           "`LCAmount` > 0; if `LCAmountSpec.Qualifier` = `X`, both tolerances are present.",
           "`ApplicableRules.Rules` = `OTHR` requires `Narrative`.",
           "Governing law: `GoverningLawCountry` is a valid ISO country; `PlaceOfJurisdiction` is not set without it.",
           "Text in `Documents` and `AdditionalConditions` uses only SWIFT-permitted characters and line length. **[Assumption]**",
           "`ChargeAccount` and `PrincipalAccount` belong to the applicant.",
           "Attachments: allowed MIME types, size limits, document type from the bank list."],
    minimum=("**[Proposed]** `Applicant`, `Beneficiary`, `LCAmount`, `ExpiryDetails`, `ApplicableRules`, `Documents` (what the beneficiary must present to claim), "
             "`Financial` or `Trade`, `ChargeAccount`, `IssuanceChgsFor`. Validate against the TI test environment."),
    samples=[('TFISBAPP', {
        'Applicant/Customer': 'ACMEIMP',
        'Beneficiary/NameAddress': 'GLOBAL DEVELOPMENTS PTE LTD\n1 RAFFLES PLACE\nSINGAPORE', 'Beneficiary/Country': 'SG',
        'LCAmount/Amount': '1000000.00', 'LCAmount/Currency': 'USD',
        'ExpiryDetails/ExpiryType': 'N', 'ExpiryDetails/Date': '2027-10-31', 'ExpiryPlace': 'SINGAPORE',
        'StandBy': 'Y', 'Financial': 'N', 'Trade': 'Y', 'PartialDrawingsAllowed': 'Y', 'MultipleDrawingsAllowed': 'Y',
        'ApplicableRules/Rules': 'ISPR', 'Documents': '+BENEFICIARY SIGNED STATEMENT THAT THE APPLICANT HAS FAILED TO PERFORM ITS OBLIGATIONS UNDER CONTRACT CT-2026-118',
        'GoverningLawDetails/GoverningLawCountry': 'SG',
        'UndertakingDeliveryDetails/DeliveryByMethod': 'COUR',
        'IssuanceChgsFor': 'A', 'OverseasChgsFor': 'A', 'ChargeAccount': '1234567890',
        'eBankMasterRef': 'CH-ISB-000045', 'eBankEvent': 'CH-ISB-000045-01'}),
        ('TFISBPYR', {'MasterRef': 'ISB2600045', 'ClaimId': 'CLM001', 'ResponseAction': None,
                      'ResponseDetails': 'Documents are in order. Approve payment.', 'NetAmountPaid/Amount': '250000.00',
                      'NetAmountPaid/Currency': 'USD', 'AuthorisationInstructions': 'PAY',
                      'eBankMasterRef': 'CH-ISB-000045', 'eBankEventRef': 'CH-ISB-000045-10'})],
    their_ref='CT-2026-118',
    tracking=("Tracking works as for the Import LC (see [Import LC specification, section 9](import-letter-of-credit.md)): gateway response, then `SearchMasters` filtered by "
              "product, then `RetrieveMasterDetail`. Standbys add two events to watch: **claims** (an outstanding claim with its `ClaimId` is the trigger for `TFISBPYR`) "
              "and **renewals** (the channel should alert the customer before the notice date computed from `Renewal`)."),
    exceptions=[('Claim arrives and the response window is short', 'Alert the maker and approvers with the deadline from the claim; escalate before the deadline; allow a single-approver fast path configured by the bank.'),
                ('Customer wants to stop a renewal', 'Send `TFISBAMR` (free-text) or `TFISBAMN` with the renewal group changed before the notice date. Show the notice date on the form.'),
                ('Beneficiary has not accepted an amendment', 'The channel shows the amendment as pending acceptance until TI reports the beneficiary response.'),
                ('Facility is insufficient', 'TI rejects or holds the event; the channel shows "Under review by bank" and, if given, the reason.')],
    acceptance=["A maker can complete the standby form, save a draft, and resume it.",
                "All schema rules in 8.1 are enforced client- and server-side with field-named messages.",
                "A maker cannot approve their own instruction; approver can approve, return or reject.",
                "Exactly one `TFISBAPP` is sent per approved instruction, and a duplicate send creates no second event in TI.",
                "The standby appears as issued, with its TI reference, after the TSO authorises it.",
                "A claim raised in TI reaches the customer as an alert and can be approved or refused with `TFISBPYR`.",
                "Renewal notice dates are computed and alerts fire before them."],
    open=["Confirm `Product` code and which `ProductType` values apply to standbys.",
          "Confirm whether `TFISBAPP` or the Import LC message (`TFILCAPP` with `StandBy` = `Y`) is the one to use for new standbys; both exist in the schema.",
          "Obtain the tailored `ResponseAction` values for demands.",
          "Decide how renewal reminders are produced (channel computation vs. TI notification).",
          "Confirm which standby wordings are available for customer selection (`UseFreeFormat`)."],
)

# --------------------------------------------------------------------------- IGT
IGT = dict(
    id='IGT', file='guarantee-issued.md', title='Guarantee Issued (Import Guarantee)',
    priority='P1, wave 2', role='Applicant (principal)', product='IGT',
    intro=("The customer, as **principal**, asks the bank to issue a **guarantee** (bid bond, performance bond, advance-payment guarantee, retention, customs or financial guarantee) "
           "in favour of a beneficiary. Payment is made on a written demand, so the beneficiary can claim without proving a default. "
           "Governed by URDG 758 or local law. The guarantee shares the standby data model, so this specification is written as a delta to "
           "[Import Standby LC](import-standby-lc.md); the guarantee-specific elements are the contract and counter-guarantee blocks, the guarantee details, and the shipping-guarantee-style fields (`LetterOfIndemnity`, `BillNumber`, `VesselName`...)."),
    out_of_scope="data take-on, SWIFT MT760 generation, and the newer Undertaking messages (see [Undertaking Issued](undertaking-issued.md)).",
    messages=[
        M('TFIGTAPP', 'Issue guarantee', 'Create / issue', baseline='TFISBAPP', baseline_label='TFISBAPP', baseline_where='[Import Standby LC, section 6.3](import-standby-lc.md)'),
        M('TFIGTAMN', 'Amend guarantee', 'Amend', baseline='TFIGTAPP', baseline_label='TFIGTAPP', baseline_where='section 6.3 of this document'),
        M('TFIGTAMR', 'Basic amendment request (free text)', 'Amend'),
        M('TFIGTCAN', 'Cancel guarantee', 'Cancel'),
        M('TFIGTPYR', 'Respond to a claim', 'Claim response'),
        M('TFIGTCOR', 'Correspondence', 'Correspondence'),
    ],
    primary='TFIGTAPP',
    scenario=("A contractor needs a performance guarantee of 10% of a contract value for a public tender. It applies for an IGT with contract details (reference, tender closing date, "
              "guarantee value percentage), an expiry tied to the contract completion, and, if the beneficiary insists on a local bank, a counter-guarantee instruction to an overseas bank. "
              "The TSO issues the guarantee and, where needed, the counter-guarantee. If the beneficiary claims, the customer answers the claim."),
    flows=[dict(kind='issue', title='Issuance', what='guarantee application', msg='TFIGTAPP', event='Guarantee Issued issue event'),
           dict(kind='respond', title='Claim received', what='claim response', msg='TFIGTPYR', event='Guarantee Issued outstanding claim event',
                trigger='Notification: claim registered on the guarantee')],
    pre=["The corporate is enabled for product `IGT`; user entitlements and limits apply.",
         "A guarantee limit exists in the bank's limit system for the customer.",
         "Reference data includes guarantee types, contract reference codes, and country codes.",
         "If a counter-guarantee is used, the counter-guarantee bank is known (address book or SWIFT directory)."],
    screens_intro="Same wizard as the standby, with two guarantee-specific steps (Contract, Counter-guarantee).",
    screens=[
        ('Basics', '`Applicant`; `Context.TheirReference`; `ApplicationDate`; template'),
        ('Beneficiary', '`Beneficiary`; `BenAccNo`; advising bank; `AdviseDirect`; `ConfirmingBank`'),
        ('Guarantee terms', '`LCAmount`; `ExpiryDetails`; `Renewal`; drawing options; `GuaranteeDetails` (**guarantee-specific**: `ReceiverOfUndertaking`, `UnlimitedExpiry`); `ApplicableRules`; `GoverningLawDetails`'),
        ('Contract', '`ContractDetails` (**guarantee-specific**): reference code and narrative, reference date, tender closing date, total order amount, guarantee value percent'),
        ('Counter-guarantee', '`CounterGuaranteeAmount`, `CounterGuaranteeExpiryDate`, `CounterGuaranteeIssBank`, `CounterGuaranteePrincipalBank`, `CustomerCounter`, `CounterGteeIdentifier` (**guarantee-specific**)'),
        ('Shipping-related (optional)', '`LetterOfIndemnity`, `BillNumber`, `FlightDetails`, `ShippingCompany`, `VesselName`, `ShipmentAdditionalInformation`'),
        ('Wording', '`UseFreeFormat`, `FreeFormatInstructionsForSWIFT`, `Documents`, `AdditionalConditions`'),
        ('Delivery', '`UndertakingDeliveryDetails`'),
        ('Charges, accounts, cover', 'As for the standby'),
        ('Instructions and documents', '`ApplicantInstructions`, instructions to advising/paying bank, attachments'),
        ('Review and submit', '')],
    hidden=HIDDEN_COMMON,
    master_note='Empty for a new guarantee; `MasterRef` for later messages.',
    channel_managed=CHANNEL_MANAGED_REFS,
    schema_rules=(SCHEMA_RULES_GENERIC +
                  "Guarantee points to watch:\n\n"
                  "* `CounterGteeIdentifier` ∈ {`C` counter received, `T` counter received – request, `R` request issuance}.\n"
                  "* `ContractDetails.GuaranteeValuePercent` is `decimal(15,12)`; do not round on the client.\n"
                  "* The element names for counter-guarantee fields differ between `TFIGTAPP` (`CounterGuaranteeIssBank`, `CounterGteeIdentifier`) and `TFIGTAMN` (`CounterGuaranteeIssuingBank`, `CounterGuaranteeIdentifier`). Take names from the schema catalogue per message.\n"
                  "* Other coded fields follow the standby (`ExpiryType` C/N/Y, `ApplicableRules.Rules`)."),
    rules=["All standby rules on expiry, renewal and amounts apply (see [Import Standby LC](import-standby-lc.md), section 8.2).",
           "`ContractDetails.ReferenceDate` is not in the future; `TenderClosingDate` is before the guarantee expiry if both are given.",
           "`GuaranteeValuePercent` is between 0 and 100; if `TotalOrderAmount` and `LCAmount` are both given they agree with the percentage within a tolerance set by the bank. **[Proposed]**",
           "If a counter-guarantee is requested, `CounterGuaranteeIssBank` is present and `CounterGuaranteeExpiryDate` is on or after the guarantee expiry. **[Proposed]**",
           "`LetterOfIndemnity` = `Y` is accepted only when shipping data is supplied. **[Proposed]**",
           "Guarantee wording: if `UseFreeFormat` = `Y`, `FreeFormatInstructionsForSWIFT` is required and SWIFT-safe."],
    minimum=("**[Proposed]** `Applicant`, `Beneficiary`, `LCAmount`, `ExpiryDetails`, `ApplicableRules`, `ContractDetails.ReferenceCode`, `ChargeAccount`, `IssuanceChgsFor`; "
             "counter-guarantee fields when requested."),
    samples=[('TFIGTAPP', {
        'Applicant/Customer': 'ACMEIMP',
        'Beneficiary/NameAddress': 'MINISTRY OF PUBLIC WORKS\nJALAN MERDEKA 1\nJAKARTA', 'Beneficiary/Country': 'ID',
        'LCAmount/Amount': '350000.00', 'LCAmount/Currency': 'USD',
        'ExpiryDetails/ExpiryType': 'N', 'ExpiryDetails/Date': '2027-12-31', 'ExpiryPlace': 'JAKARTA',
        'ContractDetails/ReferenceCode': 'CONT', 'ContractDetails/ReferenceNarrative': 'TENDER 2026/INF/044',
        'ContractDetails/TenderClosingDate': '2026-11-30', 'ContractDetails/GuaranteeValuePercent': '10.000000000000',
        'ApplicableRules/Rules': 'URDG', 'Documents': '+WRITTEN DEMAND STATING THAT THE PRINCIPAL IS IN BREACH OF TENDER 2026/INF/044',
        'IssuanceChgsFor': 'A', 'OverseasChgsFor': 'A', 'ChargeAccount': '1234567890',
        'eBankMasterRef': 'CH-IGT-000012', 'eBankEvent': 'CH-IGT-000012-01'})],
    their_ref='TENDER-2026-044',
    tracking=("As for the standby. Guarantees usually have a long life, so the channel's expiry and renewal alerts and the **claim** alert (trigger for `TFIGTPYR`) are the important status features. "
              "If a counter-guarantee is issued, TI may create a linked master; show it under the main guarantee when the enquiry exposes the link."),
    exceptions=[('Beneficiary demands extension or payment', 'Claim response carries `RequestedNewExpiryDate`; show a clear "extend or pay" choice.'),
                ('Counter-guarantee bank rejects', 'TI event is held; the channel shows "Bank action required" and the reason if exposed.'),
                ('Guarantee expires while an amendment is pending', 'Block submit of amendments that would be effective after expiry; show warning.')],
    acceptance=["A maker can complete the guarantee form including contract and counter-guarantee blocks and resume a draft.",
                "Element names are generated per message from the schema (no hand-typed names).",
                "One `TFIGTAPP` per approved instruction; no duplicates.",
                "Expiry alerts and claim alerts reach the right users.",
                "A claim can be approved or refused with `TFIGTPYR`, with optional extension date."],
    open=["Confirm the TI guarantee `ProductType` list and whether IGT or UTI is the strategic message for new business (both exist).",
          "Confirm counter-guarantee handling: does TI create a separate master?",
          "Obtain tailored `ResponseAction` values for guarantee claims.",
          "Confirm legal wording templates for customer selection."],
)

# --------------------------------------------------------------------------- UTI
UTI = dict(
    id='UTI', file='undertaking-issued.md', title='Undertaking Issued (Guarantee / Standby, new model)',
    priority='P1, wave 2 (decide IGT/ISB vs UTI first)', role='Applicant (principal)', product='UTI',
    intro=("`UTI` is the **ISO 20022-style undertaking model** in TI: one product for demand guarantees, standby LCs, dependent undertakings and counter-undertakings, "
           "with the terms held in a structured `UndertakingDetails` block (including a counter-undertaking received and one to send) instead of the Import LC field set. "
           "It is the strategic model for new guarantee business. This specification covers the customer-facing messages. "
           "If the bank offers both IGT/ISB and UTI, the channel should expose one of them per instrument type to avoid duplicated screens."),
    out_of_scope="data take-on, and the older Import Guarantee / Standby messages (see their documents).",
    messages=[
        M('TFUTIAPP', 'Issue undertaking', 'Create / issue'),
        M('TFUTIAMN', 'Amend undertaking', 'Amend', baseline='TFUTIAPP', baseline_label='TFUTIAPP', baseline_where='section 6.3 of this document'),
        M('TFUTICAN', 'Cancel undertaking', 'Cancel'),
        M('TFUTIPYR', 'Respond to a claim', 'Claim response'),
        M('TFUTICLM', 'Claim received', 'Claim / presentation', exposure='Bank-side', note='Presenter-side message: it records a claim that the beneficiary presented. Do not expose to customers. Listed so the channel can show the resulting claim.'),
        M('TFUTICOR', 'Correspondence', 'Correspondence'),
    ],
    primary='TFUTIAPP',
    scenario=("The customer requests a demand guarantee in structured form: undertaking type, rules (URDG 758), amount, expiry type and date, claim period, drawing options, "
              "governing law, text wording, delivery. For a local guarantee backed by an overseas bank the customer also supplies the counter-undertaking to send. "
              "The TSO issues the undertaking; later claims reach the customer as alerts."),
    flows=[dict(kind='issue', title='Issuance', what='undertaking application', msg='TFUTIAPP', event='Undertaking issue event'),
           dict(kind='respond', title='Claim received', what='claim response', msg='TFUTIPYR', event='Undertaking Issued outstanding claim event',
                trigger='Notification: claim registered on the undertaking')],
    pre=["Corporate enabled for `UTI`; entitlements and limits.",
         "Facility available.",
         "Reference data for undertaking forms (`DGAR`, `DEPU`, `DOCR`, `STBY`), rules codes, wording types, languages, delivery methods."],
    screens_intro="The wizard mirrors the structure of `UndertakingDetails`.",
    screens=[
        ('Parties', '`PartyDetails`: applicant, beneficiary, beneficiary account, issuing/advising/advise-through banks, principal-not-applicant, reimbursing bank, `AdviseDirect`'),
        ('Undertaking', '`UndertakingDetails/Undertaking`: form `FormOfUndertaking`, `ProductType`, rules `ApplicableRulesUndertaking`, issue date'),
        ('Amount', '`UndertakingAmount/Amount`, `AmountSpecification`, `AdditionalAmountsCoveredUndertaking`'),
        ('Validity', '`Expiry` (type, date, condition, automatic release), `ClaimPeriodExpiry`, `RenewalDetails`'),
        ('Drawing', '`DrawingDetails` (not payable before, partial, multiple), `AvailableWith`, `TransferDetails`'),
        ('Wording', '`TextOfUndertaking` (wording type, standard wording request, language), `DocumentAndPresentationInstructions`, `TermsAndConditions`, `UnderlyingTransactionDetails`'),
        ('Law and delivery', '`GoverningLaw`, `DeliveryDetails`'),
        ('Counter-undertaking', '`CounterReceived`, `CounterToSend` (same structure as `Undertaking`)'),
        ('Contract and shipment', '`ContractDetails`, `ShipmentDetails`, `HSCode`'),
        ('Confirmation', '`RequestedConfirmationDetails`'),
        ('Charges, cover', '`ChargeDetails`, `MarginDeposit`, `CollateralDetails`, `FXContracts`, `LicenseDetails`'),
        ('Instructions', '`PartyInstructions` (notes to bank), attachments'),
        ('Review and submit', '')],
    hidden=HIDDEN_COMMON + " Also `InstructionsReceived`, `RequestType`, `TypeOfLocalUndertaking`, `PartyInstructions/NotesToNonPrincipal`.",
    master_note='Empty for a new undertaking; `MasterRef` / `RelatedMasterRef` for later messages and for linked undertakings.',
    channel_managed=CHANNEL_MANAGED_REFS,
    schema_rules=(SCHEMA_RULES_GENERIC +
                  "Undertaking points to watch:\n\n"
                  "* `FormOfUndertaking` ∈ {`DGAR` demand guarantee, `DEPU` dependent undertaking, `DOCR` documentary credit, `STBY` standby}.\n"
                  "* `Expiry.ExpiryType` ∈ {`C` conditional, `N` specific date, `Y` unlimited}.\n"
                  "* `ApplicableRulesUndertaking.Code` ∈ {`ISPR`, `NONE`, `OTHR`, `UCPR`, `URDG`}; `Narrative` ≤ 35 only for `OTHR`.\n"
                  "* `DeliveryDetails.DeliveryByMethod` ∈ {`COLL`, `COUR`, `MAIL`, `MESS`, `OTHR`, `REGM`}; `DeliveryToCode` ∈ {`BENE`, `OTHR`}.\n"
                  "* `FinancialTrade` ∈ {`F`, `T`}; `TextOfUndertaking.WordingType` ∈ {`A`, `B`, `C`, `I`, `O`, `S`}; language codes are ISO 639-1.\n"
                  "* `DocumentAndPresentationInstructions` ≤ 6,700 and `TermsAndConditions` ≤ 80,400 characters."),
    rules=["Same expiry logic as the standby; `ClaimPeriodExpiry` is optional and adds to the expiry.",
           "`Undertaking/UndertakingAmount/Amount` > 0 and in a valid currency.",
           "If `TextOfUndertaking.RequestStandardWording` = `Y`, `WordingType` and `RequestLanguage` are required.",
           "`CounterToSend` is allowed only when the bank offers counter-undertakings for the customer. **[Proposed]**",
           "`PartyDetails/Beneficiary` has a name, address and country.",
           "Do not allow edits to `CounterReceived` by customers; it records what the bank received. **[Proposed]**"],
    minimum="**[Proposed]** `PartyDetails/Applicant`, `PartyDetails/Beneficiary`, `UndertakingDetails/Undertaking` with form, rules, amount and expiry, `ChargeDetails/ChargeAccount`.",
    samples=[('TFUTIAPP', {
        'PartyDetails/Applicant/Customer': 'ACMEIMP',
        'PartyDetails/Beneficiary/NameAddress': 'MINISTRY OF PUBLIC WORKS\nJALAN MERDEKA 1\nJAKARTA',
        'PartyDetails/Beneficiary/Country': 'ID',
        'UndertakingDetails/Undertaking/FormOfUndertaking': 'DGAR',
        'UndertakingDetails/Undertaking/ApplicableRulesUndertaking/Code': 'URDG',
        'UndertakingDetails/Undertaking/Expiry/ExpiryType': 'N',
        'UndertakingDetails/Undertaking/Expiry/Date': '2027-12-31',
        'UndertakingDetails/Undertaking/GoverningLaw/GoverningLawCountry': 'ID',
        'UndertakingDetails/Undertaking/FinancialTrade': 'T',
        'ChargeDetails/IssuanceChgsFor': 'A', 'ChargeDetails/ChargeAccount': '1234567890',
        'EBankMasterRef': 'CH-UTI-000003', 'EBankEventRef': 'CH-UTI-000003-01'})],
    their_ref='TENDER-2026-044',
    tracking="As for the guarantee. The amount and expiry live inside `UndertakingDetails`, so the enquiry `DefinitionID` for UTI must return those paths (02, Q3).",
    exceptions=[('Amend changes only one block', 'The message carries all blocks; the channel resends unchanged values from the current master so TI does not read missing blocks as deletions. Confirm with the TI team whether partial amendments are supported.'),
                ('Claim for a counter-undertaking', 'Appears as a claim on the linked master; show both.')],
    acceptance=["Maker can complete all `UndertakingDetails` sections and resume drafts.",
                "Element names and nesting match the schema for each message.",
                "One `TFUTIAPP` per approved instruction, no duplicates.",
                "Claims reach the customer and can be answered with `TFUTIPYR`.",
                "`TFUTICLM` is never offered to customers."],
    open=["Decide which of IGT, ISB and UTI the bank uses for new guarantees and standbys.",
          "Confirm partial-amendment semantics for `TFUTIAMN`.",
          "Confirm `Product` code and `ProductType` list.",
          "Confirm the `DefinitionID` for UTI enquiries."],
)

# --------------------------------------------------------------------------- SHG
SHG = dict(
    id='SHG', file='shipping-guarantee.md', title='Shipping Guarantee',
    priority='P1, wave 3', role='Importer (applicant under an LC, or drawee under a collection)', product='SHG',
    intro=("When goods reach the port before the original shipping documents, the importer cannot take delivery. A **shipping guarantee** is the bank's undertaking to the shipping company "
           "that it will deliver the original bill of lading when it arrives, so the carrier can release the goods. It is requested against an existing LC or collection."),
    out_of_scope="LC or collection set-up (see their documents) and release of original documents.",
    messages=[M('TFSHGAPP', 'Request shipping guarantee', 'Create / issue', phase='3'),
              M('TFSHGCOR', 'Correspondence', 'Correspondence', phase='3')],
    primary='TFSHGAPP',
    scenario=("The importer sees that the vessel has arrived but the documents have not. From the LC or collection screen it chooses *Request shipping guarantee*, enters the amount "
              "(normally the invoice value), the goods documents of title (bill of lading number, vessel, shipper), notes for the consignee and the shipping company, and the fee account. "
              "TI creates the shipping guarantee master linked to the originating transaction; the TSO issues it, and the bank hands the guarantee to the importer or the carrier."),
    flows=[dict(kind='issue', title='Request', what='shipping guarantee request', msg='TFSHGAPP', event='Shipping Guarantee create event')],
    pre=["An LC (`ILC`) or inward collection (`IDC`) exists and is live. The channel knows its reference as `OriginatingTransaction`.",
         "The customer is entitled to `SHG`.",
         "The charge/fee account is the customer's."],
    screens_intro="Single-page form launched from the originating transaction.",
    screens=[
        ('Originating transaction', '`OriginatingTransaction` (read-only, pre-filled); applicant/importer `Importer`'),
        ('Guarantee', '`GuaranteeAmount` (amount, currency); dates `ApplicationDate`, `IssueDate`, `ExpiryDate`; advice method `AdviceMethod`; `MultipleDocs`'),
        ('Goods and carrier', '`DocumentOfTitles` (repeating: bill of lading or airway bill details); `ShippingParty`; `NotesToConsignee`; `NotesToShippingCo`'),
        ('Conditions', '`AdditionalConditions`'),
        ('Fees and cover', '`FeeAccountNumber`, `ChargeAccount`, `PrincipalAccount`, `PrincipalAccountNumber`, `MarginDeposit`, `Collateral`'),
        ('Attachments and submit', 'Copy of bill of lading, invoice; review and submit')],
    hidden=HIDDEN_COMMON + " Also `SpecialFields`, `AdditionalParties` (unless the bank asks for named parties).",
    master_note='Empty: TI creates a new shipping guarantee master and links it through `OriginatingTransaction`.',
    channel_managed=CHANNEL_MANAGED_REFS,
    schema_rules=(SCHEMA_RULES_GENERIC +
                  "* `AdviceMethod` ∈ {`e` e-mail, `o` e-mail account officer, `5` fax, `a` gateway, `i` internal, `1` mail, `6` no document required, `4` SWIFT, `2` telex, `3` telex via SWIFT}. "
                  "For a customer-requested shipping guarantee `i` or `6` is typical. **[Assumption]**\n"
                  "* `NotesToConsignee`, `NotesToShippingCo` and `AdditionalConditions` are each ≤ 10,000 characters."),
    rules=["`OriginatingTransaction` exists, is live, belongs to the customer, and its product is `ILC` or `IDC`.",
           "`GuaranteeAmount` ≤ the outstanding value of the originating transaction (usually 100–110%). **[Proposed]**",
           "`ExpiryDate` is after `IssueDate` and not later than the bank's maximum validity (configuration).",
           "At least one `DocumentOfTitle` with a document number and carrier.",
           "No second active shipping guarantee for the same bill of lading unless `MultipleDocs` = `Y`. **[Proposed]**",
           "Fee and principal accounts belong to the customer."],
    minimum="**[Proposed]** `Importer`, `OriginatingTransaction`, `GuaranteeAmount`, `DocumentOfTitles` (at least one), `ShippingParty`, `FeeAccountNumber` or `ChargeAccount`.",
    samples=[('TFSHGAPP', {
        'ApplicationDate': '2026-10-18', 'ExpiryDate': '2027-01-18', 'MultipleDocs': 'N', 'AdviceMethod': 'i',
        'Importer/Customer': 'ACMEIMP', 'ShippingParty/NameAddress': 'OCEAN LINE AGENCIES\nTANJUNG PRIOK\nJAKARTA',
        'GuaranteeAmount/Amount': '250000.00', 'GuaranteeAmount/Currency': 'USD', 'OriginatingTransaction': 'ILC2600123',
        'NotesToShippingCo': 'Please release 5000 units ex MV OCEAN STAR against this guarantee.',
        'FeeAccountNumber': '1234567890', 'eBankMasterRef': 'CH-SHG-000004', 'eBankEventRef': 'CH-SHG-000004-01'})],
    their_ref='SHG-REQ-2026-04',
    tracking="Status follows the shipping guarantee master. Show the link to the originating LC or collection and the expiry. The channel should also remind the importer to settle the guarantee once the original documents arrive (documents presented under the LC).",
    exceptions=[('Documents arrive before the guarantee is issued', 'Allow the customer to cancel the draft; if already submitted, send `TFSHGCOR` to request withdrawal.'),
                ('Guarantee amount exceeds LC balance', 'Block submission with a clear message.'),
                ('Shipping company rejects the guarantee wording', 'Handled by the bank; the customer answers a bank query via correspondence.')],
    acceptance=["Request is possible only from a live LC or collection belonging to the customer.",
                "Document-of-title data is captured and sent in `DocumentOfTitles`.",
                "One `TFSHGAPP` per approved request, no duplicates.",
                "Customer sees the shipping guarantee master linked to the LC."],
    open=["Confirm that `Product` code `SHG` and the master link behaviour match TI.",
          "Confirm default `AdviceMethod`, and whether the guarantee is released to the customer or the carrier.",
          "Confirm structure of `DocumentOfTitles` (see schema) and what the TSO expects in `SpecialFields`."],
)

# --------------------------------------------------------------------------- IDC
IDC = dict(
    id='IDC', file='inward-collection.md', title='Inward (Import) Collection',
    priority='P1, wave 3', role='Drawee (importer)', product='IDC',
    intro=("An **inward collection** is a bill or documents that the seller's bank has sent to our bank for collection from our customer, the drawee. "
           "The bank creates the master when documents arrive; the customer then **accepts** or **pays** (documents against acceptance / payment), or **refuses**. "
           "This specification covers the customer's responses. The create messages (`TFIDCCRE`, `TFIDCNCAM`) are listed for completeness: the schema says the drawer sends them, "
           "which in practice means the bank records them from the incoming collection."),
    out_of_scope="outward collections (see [Outward Collection](outward-collection.md)), release of documents against trust receipt, and financing of the payment (see [Financing](financing.md)).",
    messages=[
        M('TFCOLACP', 'Accept drafts', 'Payment / acceptance', phase='3'),
        M('TFCOLPAY', 'Pay collection', 'Payment / acceptance', phase='3'),
        M('TFCOLCON', 'Continue payment', 'Payment / acceptance', phase='3'),
        M('TFCOLNACP', 'Refuse acceptance or payment', 'Response / request', phase='3'),
        M('TFIDCCOR', 'Correspondence', 'Correspondence', phase='3'),
        M('TFIDCCRE', 'Create inward collection', 'Create / issue', exposure='Review', phase='-', note='Schema: "Sent by the Drawer to the bank". Bank-side recording of an incoming collection. Expose only if the bank lets customers register an inward collection themselves.'),
        M('TFIDCNCAM', 'Create inward collection (no customisation amendment)', 'Create / issue', exposure='Review', phase='-', baseline='TFIDCCRE', baseline_label='TFIDCCRE'),
    ],
    primary='TFCOLACP',
    scenario=("Documents for USD 75,000 arrive from the seller's bank, payable 60 days after sight. TI creates the inward collection. The customer sees *Documents received* "
              "in the channel, reviews the drafts and documents (viewing copies from the DMS), and chooses: **Accept** (`TFCOLACP`) the drafts; later **Pay** at maturity (`TFCOLPAY`) "
              "or **Pay now** (`TFCOLCON` to continue payment); or **Refuse** (`TFCOLNACP`) with a reason. The TSO releases documents against the chosen action."),
    flows=[dict(kind='respond', title='Acceptance or payment', what='acceptance or payment instruction', msg='TFCOLACP / TFCOLPAY', event='Inward Collection acceptance / payment event',
                trigger='Notification: inward collection received')],
    pre=["The inward collection exists in TI and the customer is the drawee.",
         "Drafts and documents are visible to the customer (DMS access or a summary from `RetrieveMasterDetail`).",
         "For payment, the debit account is the customer's and has sufficient funds (TI checks)."],
    screens_intro="A task-driven screen opened from the collection alert.",
    screens=[
        ('Collection summary', 'Read-only: drawer, collecting/remitting bank, amount, drafts (`CollectionDraftss`), documents, tenor, charges (from `RetrieveMasterDetail`)'),
        ('Decision', 'Accept / Pay / Pay later / Refuse'),
        ('Accept', '`AcceptAllDrafts`, `AmountAccepted`, `ReceivedOnDate`, `TransferBy`, `InstructionsToSend`, `Tenor`, `ExpireCollection`'),
        ('Pay', '`CollectionAmount`, `CollectionDate`, `PaymentAction`, `PaymentDraftss`, `Financing` (optional)'),
        ('Refuse', '`NonAcceptanceAllDrafts`, `NonPaymentAllDrafts`, narrative (`MessageText` / `Narrative`)'),
        ('Review and submit', 'Approval route, debit account confirmation')],
    hidden=HIDDEN_COMMON + " Also `Function`, `FormOfUndertaking`, `EventFurtherID` (unless the bank defines customer-visible sub-types).",
    master_note='**Required.** `MasterRef` / `OurReference` is the inward collection reference.',
    channel_managed=CHANNEL_MANAGED_REFS + "\n\n" + "`eBankCorpRef` holds the channel's own reference of the collection in the customer's system, if the customer wants it carried.",
    schema_rules=(SCHEMA_RULES_GENERIC +
                  "* The `TFCOL*` response messages share the correspondence skeleton (`Sender`, `Subject`, `CorresType`, `MessageText`...) plus their own elements.\n"
                  "* `CollectionDraftss/CollectionDrafts` is repeating. `TenorPeriod` ∈ {`D`, `M`, `Q`, `W`, `Y`}, `FromAfter` ∈ {`A`, `F`, `E`}, `TenorFrom` ∈ {`A` air waybill, `G` arrival of goods, `E` bill of exchange, `L` bill of lading, `I` invoice, `X` other, `P` shipment date, `S` sight}.\n"
                  "* `DocumentsAgainst.Type` ∈ {`A` D/A, `P` D/P, `O` other, `V` pour aval}."),
    rules=["The action is allowed by the collection's current state: accept only if documents are against acceptance and not yet accepted; pay only after acceptance or when against payment.",
           "`AmountAccepted` ≤ the draft amount; partial acceptance only if the collection allows part payment (`AllowPartPayment`).",
           "`CollectionAmount` (payment) ≤ outstanding amount; payment date is today or later and a business day.",
           "A refusal needs a reason (narrative).",
           "Cut-off and maturity dates are shown; late actions carry a warning.",
           "A second action for the same draft is blocked while the first is in process."],
    minimum="**[Proposed]** `MasterRef`, `Sender`, the action elements for the chosen message, `eBankMasterRef`, `eBankEventRef`.",
    samples=[('TFCOLACP', {'MasterRef': 'IDC2600020', 'Sender/Customer': 'ACMEIMP', 'Subject': 'Acceptance',
                           'AcceptAllDrafts': 'Y', 'ReceivedOnDate': '2026-10-18', 'InstructionsToSend': 'Advise acceptance to the remitting bank.',
                           'eBankMasterRef': 'CH-IDC-000020', 'eBankEventRef': 'CH-IDC-000020-02'}),
             ('TFCOLNACP', {'MasterRef': 'IDC2600020', 'Sender/Customer': 'ACMEIMP', 'Subject': 'Non-acceptance',
                            'NonAcceptanceAllDrafts': 'Y', 'MessageText': 'Goods not in accordance with the purchase order.',
                            'eBankMasterRef': 'CH-IDC-000020', 'eBankEventRef': 'CH-IDC-000020-04'})],
    their_ref='PO-2026-0457',
    tracking="Status follows the collection master: *Documents received* → *Accepted* → *Paid* (or *Refused*). The key events to watch are the arrival of documents and drafts approaching maturity. Maturity alerts are computed from the drafts' tenor.",
    exceptions=[('Drawee accepts but insufficient funds at maturity', 'TI handles the debit; the channel shows the shortfall as a bank query.'),
                ('Documents discrepant', 'Discrepancy notices come through correspondence; the customer replies via `TFIDCCOR` or refuses with `TFCOLNACP`.'),
                ('Seller\'s bank recalls the collection', 'Status becomes *Cancelled*; actions are blocked.')],
    acceptance=["Customer is alerted to an inward collection and can see drafts and documents.",
                "Accept, pay, continue payment and refuse each produce exactly one correct message.",
                "Actions that do not fit the collection state are blocked in the UI.",
                "Status updates after the TSO processes the action."],
    open=["Confirm whether customers see the original documents or only a summary.",
          "Confirm the semantics of `TFCOLCON` vs. `TFCOLPAY` (continue payment vs. direct payment).",
          "Obtain tailored `PaymentAction` values.",
          "Confirm how `V` (pour aval) acceptance is handled."],
)

# --------------------------------------------------------------------------- IOA
IOA = dict(
    id='IOA', file='import-open-account.md', title='Import Open Account (Buyer side, TMA)',
    priority='P3', role='Buyer', product='IOA',
    intro=("**Open account** means the seller ships first and is paid later, with no bank instrument. The bank adds value by matching purchase orders, invoices and shipment data "
           "(SWIFT TSU / **TMA**, transaction matching application) and, optionally, by **paying** or **financing** against the matched data (a Bank Payment Obligation). "
           "The buyer's messages cover amending or cancelling the baseline, responding to the seller's requests, and requesting or answering payment."),
    out_of_scope="Seller-side messages (see [Export Open Account](export-open-account.md)), and financing of the payment (see [Financing](financing.md)).",
    messages=[
        M('TFIOACRE', 'Create baseline', 'Create / issue', exposure='Review', phase='5', note='Creates the import open-account master with buyer, seller, amounts, goods, obligation. In a TMA flow this is driven by the matched baseline; confirm who sends it.'),
        M('TFIOAAME', 'Buyer amends baseline', 'Amend', phase='5'),
        M('TFIOABRE', 'Buyer amendment request', 'Response / request', phase='5'),
        M('TFIOABRC', 'Buyer cancellation request', 'Response / request', phase='5'),
        M('TFIOACAR', 'Cancel', 'Cancel', phase='5'),
        M('TFIOASRE', 'Seller amendment request (recorded)', 'Response / request', exposure='Review', phase='5'),
        M('TFIOASRC', 'Seller cancellation request (recorded)', 'Response / request', exposure='Review', phase='5'),
        M('TFIOARSE', 'Buyer response to seller amendment request', 'Response / request', phase='5'),
        M('TFIOARSC', 'Buyer response to seller cancellation request', 'Response / request', phase='5'),
        M('TFIOARBC', 'TMA response to buyer cancellation request', 'Response / request', exposure='Bank-side', phase='-'),
        M('TFIOATBE', 'TMA response to buyer amendment request', 'Response / request', exposure='Bank-side', phase='-'),
        M('TFIOATSC', 'TMA response to seller cancellation request', 'Response / request', exposure='Bank-side', phase='-'),
        M('TFIOATSE', 'TMA response to seller amendment request', 'Response / request', exposure='Bank-side', phase='-'),
        M('TFIOARPY', 'Request payment', 'Payment / acceptance', phase='5'),
        M('TFIOAPBR', 'Buyer response to payment request mismatches', 'Response / request', phase='5'),
        M('TFIOAPGR', 'General response to payment request', 'Response / request', phase='5'),
        M('TFIOAPTR', 'TMA notification of mismatch accepted or rejected', 'Response / request', exposure='Bank-side', phase='-'),
    ],
    primary='TFIOACRE',
    scenario=("A buyer agrees a baseline with a seller: purchase order PO-2026-0457, USD 180,000, shipment from Shenzhen to Jakarta, payment 60 days after shipment. "
              "The baseline is registered in TI (and matched in the TMA). Later the seller asks to extend the shipment date; the buyer **accepts** (`TFIOARSE`). "
              "When the seller presents the invoice and shipment data, TI matches them; on a mismatch the buyer sees the discrepancies and **responds** (`TFIOAPBR`), "
              "and when matched the payment obligation is settled."),
    flows=[dict(kind='respond', title='Seller asks for an amendment', what='response to seller amendment request', msg='TFIOARSE', event='Import Open Account buyer response to seller amendment request event',
                trigger='Notification: seller amendment request received')],
    pre=["Open account is enabled for the customer and a master exists.",
         "TMA connectivity is in place if the bank offers matching.",
         "The buyer knows the purchase-order and matching references (`PurchaseOrderReference`, `TransactionMatchingReference`)."],
    screens_intro="Open-account transactions are an event stream on a master. The channel presents a timeline and the next action.",
    screens=[
        ('Baseline', '`Buyer`, `Seller`, `OpenAccountAmount`, `IssueDate`, `ExpiryDate`, `ShipmentFrom/To`, `PortOfLoading/Discharge`, `PartShipment`, `Goods`, `GoodsCode`, `ObligationDetails`, `ObligationPaymentTerms`'),
        ('Requests', 'Amend or cancel (buyer-initiated): `TFIOAAME`/`TFIOABRE`, `TFIOABRC`; respond to the seller: `TFIOARSE`/`TFIOARSC`'),
        ('Payment', 'Payment request and mismatch responses: `TFIOARPY`, `TFIOAPBR`, `TFIOAPGR`'),
        ('Charges and accounts', '`OurCharges`, `OverseaChargesFor`, `PreferredCcy`, `PrincipalAccount`, `ChargeAccount`'),
        ('Attachments and submit', 'Purchase order, invoice, transport documents')],
    hidden=HIDDEN_COMMON + " Also `MatchStatus`, `BankAction`, `BPOIncluded`, `TMAUsed`, `TransactionMatchingReference` (set from the master).",
    master_note='**Required** for all messages except the baseline: `MasterRef` is the open-account master.',
    channel_managed=CHANNEL_MANAGED_REFS,
    schema_rules=SCHEMA_RULES_GENERIC + "Response messages use `ResponseType` ∈ {`A` accepted, `R` rejected}. Amounts in `OpenAccountAmount` are strings (≤ 22).",
    rules=["Only actions valid for the current state of the baseline are shown (for example, no amendment request while another is pending).",
           "A response must refer to the original request (`CancellationRef`, `AmendmentDate`, or the request event).",
           "Payment responses must list all open discrepancies.",
           "Changes to amount, shipment dates or goods are shown as a before/after comparison."],
    minimum="**[Proposed]** `MasterRef`, `PurchaseOrderReference`, `eBankMasterRef`, `eBankEventRef`, plus the action's own elements.",
    samples=[('TFIOARSE', {'MasterRef': 'IOA2600007', 'Sender/Customer': 'ACMEIMP', 'PurchaseOrderReference': 'PO-2026-0457',
                           'TMAUsed': 'Y', 'TransactionMatchingReference': 'TMA-99817', 'ResponseType': 'A',
                           'ResponseNarrative': 'Buyer agrees to the new shipment date.', 'ResponseDate': '2026-10-18',
                           'eBankMasterRef': 'CH-IOA-000007', 'eBankEventRef': 'CH-IOA-000007-02'})],
    their_ref='PO-2026-0457',
    tracking="Show a timeline of events on the master. Statuses include *Baseline matched*, *Amendment requested*, *Mismatch*, *Payment due*, *Settled*. Mismatch detail comes from the master detail enquiry.",
    exceptions=[('Seller and buyer submit conflicting amendments', 'TI or TMA decides; the channel shows the pending request and blocks a new one.'),
                ('Mismatch not answered in time', 'Alert with deadline; the bank may reject by default.')],
    acceptance=["Buyer can see the baseline and its event timeline.",
                "Each buyer action produces one correct message for the current state.",
                "Mismatch responses list the discrepancies and the buyer's decision.",
                "Status updates after the bank processes the event."],
    open=["Which party sends the baseline (`TFIOACRE`): buyer, seller, TMA, or the bank?",
          "Are BPO obligations (`ObligationDetails`) offered to customers?",
          "Confirm the TMA role names (TMA response messages are system-to-system).",
          "Confirm customer demand for open account in the first release."],
)

PRODUCTS = [ISB, IGT, UTI, SHG, IDC, IOA]
