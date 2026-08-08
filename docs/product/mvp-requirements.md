# HomeOps AI MVP Requirements

## Purpose

Define a deliberately small first release of HomeOps AI that proves the core value proposition: helping a household keep a structured, useful record of vehicle ownership and related maintenance activity with less effort than manual tracking.

The MVP should be vehicle-first, while preserving a generic asset domain model so the product can expand into other asset types later without redesign.

## Product Goal

Enable a single household to maintain a basic ownership record for vehicles and a small set of other physical assets, including supporting documents, maintenance history, and upcoming or overdue maintenance reminders.

## MVP Hypothesis

If a user can create an account, create one household, add a vehicle, upload a supporting document, record maintenance activity, and see a basic reminder for upcoming or overdue maintenance, then the product has demonstrated the core value of HomeOps AI.

## Primary Target User

The MVP is designed for one individual or family managing their own vehicles and a small number of other household assets.

## Hero MVP Workflow

The hero workflow for the MVP is:

1. Create an account.
2. Create one household.
3. Add a vehicle with basic details.
4. Upload a manual document such as a receipt, invoice, or service record.
5. Record a maintenance event and define a basic maintenance schedule.
6. View the vehicle record and see relevant reminders.

## Core MVP User Journeys

### 1. Account and household setup
A new user can create an account, create a single household, and access a simple dashboard.

### 2. Vehicle record creation
A user can add a vehicle with essential information such as:

- make
- model
- year
- purchase date
- purchase cost
- current mileage or odometer reading
- notes

### 3. Generic asset creation
A user can create a simple generic asset record for non-vehicle items, using a basic shared schema that can later support additional asset types.

### 4. Document attachment
A user can manually upload and associate supporting documents such as:

- receipts
- invoices
- service records
- warranties
- manuals

### 5. Maintenance tracking
A user can record a maintenance event and define a maintenance schedule for recurring service.

### 6. Reminder review
A user can view upcoming and overdue maintenance reminders based on configured schedules.

## In-Scope Capabilities

The MVP includes the following capabilities:

- user account creation and sign-in
- one household per account
- vehicle-centric workflows as the primary experience
- a generic asset domain model for future expansion
- asset creation, editing, and deletion
- manual document upload and storage
- association of documents to assets
- maintenance event entry
- basic maintenance schedules
- basic upcoming and overdue maintenance reminders
- simple asset listing and search
- a basic dashboard for household records
- secure storage of household and asset information

## Explicitly Out of Scope / Non-Goals

The following are intentionally excluded from the MVP:

- AI-assisted document extraction or OCR
- mobile applications
- VIN decoding
- subscription billing and payment processing
- advanced household collaboration or multi-household sharing
- integrations with dealers, insurers, manufacturers, or service providers
- predictive maintenance beyond simple reminder logic
- advanced analytics or reporting
- AWS-specific implementation decisions

## MVP Success Criteria

The MVP is successful if all of the following are true:

- a first-time user can complete the core happy path from signup to vehicle record creation without assistance
- a user can create at least one vehicle, one document, one maintenance event, and one maintenance schedule
- the system can display at least one upcoming or overdue reminder after a maintenance schedule is created
- the user can sign out, sign back in, and retrieve the previously created vehicle, document, maintenance event, schedule, and reminder
- the experience demonstrates clear value as a replacement for manual spreadsheet-style tracking for basic household ownership records

These criteria are more objective because they are tied to concrete user actions and visible system outcomes.

## Dependencies and Assumptions

The MVP assumes:

- the initial product will be delivered as a web-based experience
- the first release focuses on one household and a narrow set of core workflows
- document uploads are handled manually by the user
- reminder logic is basic and rule-based rather than predictive
- AI and OCR are deferred to a later release
- the domain model is intentionally simple but extensible

## Product and Engineering Notes

This MVP should stay intentionally small. The implementation should prioritize:

- a clean domain model for users, households, assets, documents, maintenance events, and schedules
- straightforward workflows that can be tested end to end
- secure storage for documents and household records
- basic observability and error handling
- a foundation that can later support AI-assisted features without redesign

## Open Questions

The following decisions should be resolved before implementation begins:

- which vehicle fields are mandatory versus optional in the first release
- which non-vehicle asset categories should be supported in the MVP
- maintenance schedules may be date-based, mileage-based, or both; when both are configured, maintenance becomes due when either threshold is reached
- whether the initial dashboard should emphasize vehicles, reminders, or both

## Release Readiness Checklist

The issue can be considered complete when:

- the MVP requirements are documented in the product docs area
- the scope is clear and specific
- in-scope and out-of-scope capabilities are explicitly stated
- success criteria are defined and measurable
- assumptions and open questions are captured
- the document is ready for review in a pull request
