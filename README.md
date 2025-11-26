# UniForum – OOD Final Project

## Project Motivation & Goal
Managing academic challenges and staying connected with peers, TAs, and alumni can be overwhelming for students. Although several tools exist, the solutions are scattered across multiple platforms:

- To **connect with alumni**, students must navigate separate Alumni pages.  
- To **get doubts resolved**, students are limited to TA office hours or scheduled Zoom sessions.  
- To **seek advice from seniors** who have already taken the course, there is no dedicated platform, requiring students to rely on personal connections.  
- The **same queries** often have to be explained by TAs to multiple students repeatedly, with no centralized documentation.  
- To **connect with peers**, students often have to switch between platforms like Teams, without knowing who has expertise in specific topics.  

These solutions make learning inefficient and time-consuming.  

**UniForum** addresses these challenges by providing a unified, user-friendly platform where students can form course communities, post queries, schedule calls, and connect with peers and alumni all in one place.

## Key Features
- **Course Communities:** Join course specific communities to interact with peers.  
- **Post Queries & Discussions:** Create posts with tags such as FAQ, Installation Issues, Project Discussion, etc.  
- **Peer & Alumni Guidance:** Schedule calls with current students or connect with alumni for guidance on courses.  
- **Integrated Experience:** Combines features from Teams, Reddit, Google Meet,LinkedIn and Email for seamless collaboration.  
- **Self-Paced Learning:** Access resources and resolve doubts anytime, supporting independent learning.  

UniForum addresses students pain points and provides collaborative, supportive and efficient academic environment for Students.

## File Structure

    UniForum
    ├── src                              # Source code
    │   ├── application                  # Main application entry and styles
    │   └── edu
    │       └── northeastern
    │           └── uniforum
    │               ├── db               # Database related source files
    │               └── forum
    │                   ├── controller   # Handles user interactions
    │                   ├── dao          # Data Access Objects for DB operations
    │                   ├── model        # Data models
    │                   ├── service      # Business logic and services
    │                   ├── util         # Utility classes
    │                   └── view         # FXML views and CSS
    ├── db                               # Database files
    ├── lib                              # External libraries and JAR dependencies
    ├── tokens                           # OAuth 2.0 client credentials storage
    ├── client_secret.json               # OAuth client credentials (Google API)
    └── README.md                        # Project README file


