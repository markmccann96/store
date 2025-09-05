# Architectural Decision Records (ADR)

This directory contains the **Architectural Decision Records** (ADRs) for this application.  
ADRs capture the significant architectural decisions made, along with their context and consequences.

We follow the template and style from:  
[Jeff Tyree and Art Akerman Decision Record Template](https://github.com/joelparkerhenderson/architecture-decision-record/tree/main/locales/en/templates/decision-record-template-by-jeff-tyree-and-art-akerman)

Each ADR is a Markdown file stored in this directory.

---

## Naming convention

- ADR files are named using the format:  
  `ADRNNNN-title.md`  
  where:
    - `NNNN` is a zero-padded sequence number (e.g., `0001`, `0002`).
    - `title` is a short, kebab-case description of the decision.

- Example:  
  `ADR0001-contract-first-api.md`

---

## How to create a new ADR

1. Copy the [ADR template](ADR-template.md) into a new file.
2. Name the file according to the naming convention above.
3. Fill in the sections with your decision details.
4. Add a link to the new ADR in the **Index of ADRs** below.

---

## Index of ADRs

- [ADR0001 - Contract First API](ADR0001-contract-first-api.md)

---

## Templates

- [ADR Template](ADR-template.md) — Use this when creating a new ADR.

---

## Further Reading

- [Architecture Decision Record (ADR) GitHub repo](https://github.com/joelparkerhenderson/architecture-decision-record)
- [Template by Jeff Tyree and Art Akerman](https://github.com/joelparkerhenderson/architecture-decision-record/tree/main/locales/en/templates/decision-record-template-by-jeff-tyree-and-art-akerman)
