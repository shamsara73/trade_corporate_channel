"""Small helpers for the product definitions."""


def M(code, title, kind, exposure='Customer', phase='2', **kw):
    d = dict(code=code, title=title, kind=kind, exposure=exposure, phase=phase)
    d.update(kw)
    return d


SCHEMA_RULES_GENERIC = (
    "Lengths, data types and the allowed values of coded fields are in the tables in sections 6 and 7 and are the source for the channel's "
    "client and server validators. Dates are ISO `xsd:date`. Boolean elements carry `Y` or `N`. Amounts are strings (maximum 22 characters); "
    "agree the decimal format with the TI team. "
)

SENDER_NOTE = ("`Sender` is the party block of the instructing customer. Use the customer mnemonic; the channel fills the rest from the corporate profile.")

HIDDEN_COMMON = ("`Context.Team`, `Context.Step`, `Context.ExchangeNonAutoComplete`, `Context.BehalfOfBranch*`, `ExternalReview`, `ProvisionalFlags`, "
                 "`BankCodes`, `ContinueEvent`, `Provisional`, `AutoCreateFollowOnEvent`, `FinalWording`, `TakeonExtra`, `InternalMasterRefSequence`, "
                 "`OutstandingAmount`, `OutstandingAdditionalAmount`, `OriginalMasterReference`, `PreallocatedNo`. They affect routing, bank processing or "
                 "data take-on, and should be set by channel configuration or left to TI defaults. Confirm each with the TI team (02, Q10).")

CHANNEL_MANAGED_REFS = (
    "| Element | Value |\n|---|---|\n"
    "| `eBankMasterRef` / `EBankMasterRef` (max 20) | The channel's **transaction number**. Constant for every later event on it. **Confirmed by the bank (2026-10-23).** |\n"
    "| `eBankEvent` / `eBankEventRef` / `EBankEventRef` (max 20) | The channel's **event number** for this event (issue, claim, amendment, ...). Unique per instruction; the key for idempotent retries. **Confirmed by the bank (2026-10-23).** |\n"
    "| `TheirRef` (where the message has it, max 35) | **Message reference: unique for every message delivered**, so a re-send of the same event gets a new `TheirRef` but keeps `eBankEventRef`. With no `ServiceRequest` envelope (Q33) it does the job of `CorrelationId`. **Confirmed by the bank (2026-10-23).** |\n"
    "| `EmbeddedItemss` | Inline attachments (`ID`, `AttachType`, `DocType`, `Description`, `FileName`, `DataStream`, `MimeType`, `DocumentFileName`); `IsCustomerAttachedDocument` = `Y`. |\n"
    "| `DocumentsReceiveds` | References to documents already in the DMS, if the upload-first approach is chosen (02, Q8). |\n\n"
    "**Capitalisation differs between messages** (`eBankMasterRef` in some, `EBankMasterRef` in others; `eBankEvent`, `eBankEventRef` and `EBankEventRef`). "
    "The channel generates names from the schema catalogue and must never hand-type them."
)
