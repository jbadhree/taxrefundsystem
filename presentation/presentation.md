# Tax Refund System Design Presentation

## Table of Contents
1. [About Me](#about-me)
2. [Professional Acheivements](#professional-achievements)
3. [System Design](#system-design)
    - HLD
    - LLD
4. [Q&A Session](#qa-session)

---

## About Me

### Personal Life
• Live in Atlanta area with my spouse and 2 young kids

• I Work from home and Travel occasionally for work

### Geographic Journey
• **Grew up in Coimbatore, India** - where I discovered my love for technology

• **Started career in Pune, India** - learned fundamentals of software development

• **Moved to US in 2012** for work opportunities

• **Lived in Bay Area** - immersed in Silicon Valley tech culture

• **Moved to Chicago** - experienced different tech ecosystem

• **Currently in Atlanta** - workrd for 3 different companies, gained solid knowledge about technology and business

### Professional Passions
• **Love working with young engineers** - helping them grow and develop

• **Active in hackathons** - attend and participate regularly

• **Co-judge at hackathons** - witness incredible creativity and innovation

• **Networking events** - building connections and learning from peers

### Intellectual Interests
• **Philosophy enthusiast** - especially Epistemology (study of knowledge)

• **Karl Popper** - his ideas about falsifiability influence my problem-solving

• **Economics and Business** - fascinated by intersection with technology

• **Systems thinking** - understanding how both software and economic systems work

### Personal Hobbies
• **Blogging** - though admittedly inconsistent (working on it!)

• **Running 5Ks** - especially ones that end with celebratory drinks

• **Reading** - philosophy, economics, and business books

## Did you notice a theme ?

I seem to like **Marathons**, **Hackathons** and **Readathons** 😄

---

## Professional Achievements

### Credit Externalization Platform - KKR & PayPal Partnership
• **Project Overview**: 
    - Led the design of a platform for KKR's acquisition of €40bn European BNPL loans from PayPal
    - Converted this in to a re-useable platform which is currently being used for Blue Owl, an investor in US

• **Business Impact**: Enabled PayPal to free up capital while KKR gained access to diversified consumer loan portfolio

• **Technical Components**:
  - **BigQuery Data Lake**: Centralized data storage, Analytics and reporting 
  - **Business UI**: A frontend where business sets the rules and tracks progress and communicates with backend
  - **GKE Batch Jobs**: Config Based eligibility filters and report criteria. Daily, Weekly and Monthly Jobs
  - **Risk Platform Integration**: Connected with PayPal Risk Platform
  - **Client Systems Interface**: Seamless data transfer and reporting to investment company's systems

• **Partnership Success**: Transaction successfully closed in 2023-2024

• **Platform Reused**: Currently being re-used in US and In discussions in other markets

• **References**: 
  - [PayPal-KKR Deal Announcement](https://www.fintechfutures.com/bnpl-payments/paypal-to-sell-up-to-40bn-of-european-bnpl-loans-to-kkr)
  - [KKR Asset-Based Finance Insights](https://www.kkr.com/insights/asset-based-finance-buy-now-pay-later)

### Predictive Test Selection Platform
• **Project Origin**: Started as side project after reading Meta's research paper on predictive test selection

• **Problem Identified**: Tests failing due to irrelevant issues, wasting CI/CD resources and developer time

• **Leadership Role**: Led initiative to build intelligent test selection system

• **Key Features Implemented**: [Detailed Feature List](predictive-test-selection-features.md)

• **Technical Implementation**:
  - **ML Model**: XGBoost for tabular data with mixed-type features
  - **Inference Service**: FastAPI-based REST API for real-time predictions
  - **Integration**: Seamless CI/CD integration with build systems

• **Operational Excellence**:
  - **Model Versioning**: Joblib export with rollback capabilities
  - **Retraining**: Automated monthly/monthly model updates

• **Research Foundation**: Based on [Meta's Predictive Test Selection Research](https://research.facebook.com/publications/predictive-test-selection/)



---

## System Design: Tax Refund Status Service

### Problem Statement

[Problem Statement](problem_statement.md)

Focus Areas - Efficiency, Reliability, Secure, Highly Available, Usability

### Requirements 

Functional Requirements:
1. User goes to our web, and checks refund status
2. If its refunded, it must say so
3. Else, it should show the predicted date of refund
4. If there are any problems with refund, it should show next steps
5. Data Pipeline for AI Model


Security Requirements
1. Users must be authenticated and authorized
2. All services involved must have an authentication and authorization mechanism 
3. There must be minimal public internet exposure
4. PII must be encrypted at rest and in transit 


Efficiency requirement:
Each service must have low latency (<200ms) even when there is high traffic 
The whole solution must be highly available - 99.99% and zero down time deployments

Reliability Requirements
Info shown to the customers must be accurate / near real time
Our System must be reliable as well - Even if parts of the system fail, rest should work

Observability
1. Alerts when there is an issue
2. Easy to troubleshoot the issues

Testability 
1. Each component must be individually testable
2. Strong integration tests

### High Level Design

Here are the different components of the system

![High Level Design](hld-components.png) 

Lets Make this design secure

- Network Security
- Cloud VPCs
- Cloud Security 
- Applicaiton Security JWT auth
- Data Security 

Performance 
- Redis
- Loose Coupling 

### Data Pipeline 

### Low Level Design 

Data Model 

API Design 

### Specific Details

- ML Model
- Batch Job concurrency 



---

*Thank you for your attention. I'm excited to discuss any questions you might have about this system design or my professional experience.*
