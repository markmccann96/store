# ADR0001: Contract First API

## Status
Proposed

## Context
We need a consistent approach for designing and documenting APIs.  
The choice is between **code-first** (writing implementation and generating contracts) and **contract-first** (defining the interface/contract before implementation).
Currently, the OpenAPI.yaml contains extra endpoints compared to the implementation to keep them inline we need 
to decide how we are going to keep the two implementations in line. 

## Decision
We will adopt a **contract-first** approach for our APIs.  
API specifications (e.g., OpenAPI/Swagger) will be created and agreed upon before implementation begins. This is 
consistent with the new api being defined first in the OpenAPI.yaml file.
Using an interface based generation (rather than delegate) to minimize impact on the current implementation.

## Consequences
- Ensures API contracts are well-defined and versioned before coding starts.
- Improves alignment between teams (front-end, back-end, third parties).
- May increase initial effort up front, but reduces rework and integration issues later.
- Tooling for contract validation and generation will need to be integrated into the build pipeline. We will include generation code in the gradle tool chain to support this.
- To support the generation modified the tags in the openAPI.yaml to tag the orders, customer and positions with different tags
