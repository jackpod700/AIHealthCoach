# Architecture

This is the high-level map of how requests move through AI Health Coach. Keep detailed feature decisions in feature docs.

## Application Shape

- Frontend: Vue/Vite
- Backend: Spring Boot
- DB: PostgreSQL
- Cache/session support: Redis
- AI: Chat flow currently uses `AiChatService -> ChatClient`; future LLM boundaries should stay behind service abstractions.

```mermaid
flowchart LR
    User[User]
    Frontend[Vue/Vite Frontend]
    Backend[Spring Boot Backend]
    Database[(PostgreSQL)]
    Redis[(Redis)]
    AI[OpenAI / LLM Provider]

    User --> Frontend
    Frontend --> Backend
    Backend --> Database
    Backend --> Redis
    Backend --> AI
```

## Backend Layer Rule

Controller handles the HTTP boundary, Service owns business flow, Mapper owns database access.

```mermaid
flowchart LR
    Controller[Controller]
    Service[Service]
    Mapper[Mapper Interface]
    Xml[MyBatis XML]
    DB[(PostgreSQL)]

    Controller --> Service
    Service --> Mapper
    Mapper --> Xml
    Xml --> DB
```

## AI Chat High-Level Flow

Current chat flow records the user message, calls AI, saves the assistant message, and returns chat plus extracted proposals.

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant C as ChatController
    participant CS as ChatService
    participant AIS as AiChatService
    participant PF as AiPromptFactory
    participant LLM as ChatClient / LLM
    participant PS as Proposal Services
    participant DB as PostgreSQL

    U->>FE: Send chat message
    FE->>C: POST /api/chat/messages
    C->>CS: Save user message
    CS->>DB: insert chat_messages
    C->>AIS: Generate AI result
    AIS->>PF: Build prompt
    AIS->>LLM: Call model
    LLM-->>AIS: Assistant text + extracted data
    C->>CS: Save assistant message
    CS->>DB: insert chat_messages
    C->>PS: Build meal/exercise/weight proposals
    C-->>FE: Chat response + proposals
    FE-->>U: Render assistant response
```

## Domain API Shape

Most feature APIs follow the same broad path.

```mermaid
flowchart TD
    Request[Authenticated HTTP Request]
    Controller[Domain Controller]
    Service[Domain Service]
    Mapper[Domain Mapper]
    DB[(PostgreSQL)]
    Response[ApiResponse-wrapped JSON]
    Error[GlobalExceptionHandler]

    Request --> Controller
    Controller --> Service
    Service --> Mapper
    Mapper --> DB
    DB --> Mapper
    Mapper --> Service
    Service --> Controller
    Controller --> Response

    Controller -. validation/business error .-> Error
    Service -. business error .-> Error
    Error --> Response
```

