# githubneo
## Graph Based Link Prediction In A Social Collaboration Network with Neo4j

This project implements collaborative filtering using a graph traversal pattern to generate recommendations for GitHub
user follows. More information is available in [this paper](http://lyonwj.com).

## Data

GitHub follow data was collected from [Githubarchive.org](http://githubarchive.org) for the period April 1, 2013 - April 1, 2014.
This data was inserted into a Neo4j graph database instance for analysis. All user data is anonymized and is stored in Neo4j format
in the `data` directory in this project.

## Dependencies

Maven - this project uses Maven for dependency resolution and building, install Maven first:

* `sudo apt-get update`
* `sudo apt-get install maven`

## How to run

* Clone this repository:
    `git clone https://github.com/johnymontana/githubneo.git`
* cd into the githubneo directory:
    `cd githubneo`
* Run with maven:
    `mvn exec:java -Dexec.mainClass="com.example.Main"`

The default configuration will run cross fold validation using random samples of the GitHub user graph for various
configurations of the parameters. **NOTE: this will take a while to run.**


