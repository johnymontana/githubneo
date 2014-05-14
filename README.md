# githubneo
## Graph Based Link Prediction In A Social Collaboration Network with Neo4j

This project implements collaborative filtering using a graph traversal pattern to generate recommendations for GitHub
user follows. More information is available in [this paper](http://lyonwj.com).

## Data

GitHub follow data was collected from [Githubarchive.org](http://githubarchive.org) for the period April 1, 2013 - April 1, 2014.
This data was anonymized and serialized to edgelist format:

    1	2
    3	4
    5	6
    7	8
    9	10
    11	12
    11	13
    14	15
    16	17
    18	19

Where the first column corresponds to the user id of the source id, the second column indicates the destination user id.
The example above therefore represents user 1 follows user 2, user 3 follows user 4... The edgelist file for this project
is `data/git_follow/edgelist` in this repository.

This data was then inserted into a Neo4j graph database instance for analysis. All user data is anonymized and is stored in Neo4j format
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
    `mvn exec:java -Dexec.mainClass="RecommendFollows"`

The default configuration will run cross fold validation using random samples of the GitHub user graph for various
configurations of the parameters. **NOTE: this will take a while to run.**


