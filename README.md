# BetterNotes: A note-taking app that is not just good, but _better_.

A desktop note-taking app specialized for Math and Computer Science notes.


![image](https://github.com/user-attachments/assets/7a17032e-7bcb-4321-a62c-4688eeac3c46)

## Project Description
BetterNotes is a note-taking platform designed for students in math and computer science courses who take notes virtually. 
It supports a wide range of content formats—including Markdown, code blocks, math equations, media, graphs, and free-form 
canvases—making it easy to capture complex ideas. 
To help students stay organized, BetterNotes presents their notes in a structured, 
mind map–like view that reflects the flow and hierarchy of their thoughts.

## Building from source
We use MongoDB to store data for this project. \
To recreate the database, ensure the your cluster has a database with the name `cs346-users-db` which has 4 collections: `boards`, `contentblocks`, `notes`, `users` (they can start out empty).

### .env file
To run the project locally from source, you will need a `.env` file, which should contain a variable named `CONNECTION_STRING` which is the full connection string given by MongoDB. \
To build from source using IntelliJ, please run it from the desktop folder!

## Binaries 
Find our latest release and install the executable for your Operating System.

We currently support:
- Linux (Debian)
- MacOS
- Windows

and have executables for all of the following in our [releases](https://github.com/asterbot/BetterNotes/releases)


## Screenshots
Here are the different pages of BetterNotes!
| ![image](https://github.com/user-attachments/assets/8d465b04-b71c-4289-b04f-8312bc2356e7)|![image](https://github.com/user-attachments/assets/ab9a2cc2-3fe7-4b2b-83fe-c10ee747f9bc) | ![image](https://github.com/user-attachments/assets/1e9d260a-001d-4cdc-8982-42b747a8b56d) |
|-|-|-|
| ![image](https://github.com/user-attachments/assets/be3055df-3979-49ca-ae2d-5f9883d08000) |![image](https://github.com/user-attachments/assets/b7b71a86-bc93-4882-bad4-82ba0704f178) |![image](https://github.com/user-attachments/assets/f6744716-481b-4820-9a51-f345bb832fd3)
|![image](https://github.com/user-attachments/assets/86f4b85f-d4dc-4101-b317-bf798aa4f060) |![image](https://github.com/user-attachments/assets/217dbb1e-aa83-4ffe-be98-afa877698061) | ![image](https://github.com/user-attachments/assets/744dfc40-8ef6-45e3-8de9-1d68407cdc7d)
|![image](https://github.com/user-attachments/assets/e8858b99-f1c3-4c76-87e2-0db0aabfb7dc) |![image](https://github.com/user-attachments/assets/9c8c32bf-febb-4fbf-8637-48979e6ae37a) |![image](https://github.com/user-attachments/assets/81831e50-5f82-417d-9360-5510e0676099)


## Contributors
  |Name|GitHub|
  |-|-|
  |Arjun Sodhi| [asterbot](https://github.com/asterbot/) 
  |Cindy Li| [cindehaa](https://github.com/cindehaa)
  |Derek Yuan| [InvertedCarrot](https://github.com/InvertedCarrot/)
  |Jeri Fan| [jerifanx](https://github.com/jerifanx/)

## Additional Information

### FDGLayout Module

As part of our app development, we created a force-directed graph layout module. The .jar file is located [here](fdg_layout/jar)
with a sample usage example in the [fdg_layout_sample](fdg_layout_sample) directory of this repository. 
