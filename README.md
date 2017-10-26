# About this project
This project is a fork of the [MACEPA/EpiSample project](https://github.com/MACEPA/EpiSample). The initial aim of this 
project is to create a detailed documentation about the EpiSample application. This documentation is intended for those
institutions who like to use EpiSample for a Household Survey. This documentation includes:
- General information about EpiSample.
- How to install and configure the EpiSample on an Android device.
- How to setup the ODK Aggregate server for aggregating all the data collected with the EpiSample App.
- A complete specification of the EpiSample components and modules.
- Developers information.

# About this documentation
This documentation aims to complete and complement the original documentation
provided by MACEPA in the [Wiki of the GitHub project repository](https://github.com/MACEPA/EpiSample-Old/wiki). Therefore,
some chunks of this document are directly copied and pasted in here.

# About EpiSample
According the [EpiSample documentation posted on GitHub by MACEPA](https://github.com/MACEPA/EpiSample-Old/wiki) (Tim McDonald):

> EpiSample is a new Android application that builds off of [ODK 2.0 tools](https://opendatakit.org/use/2_0_tools/) and offers additional census,
sampling, and survey capabilities that facilitate the implementation of population-based epidemiological surveys. EpiSample features are largely based
on [GPS2](https://www.ncbi.nlm.nih.gov/pubmed/17690421), a Windows Mobile
application that was designed and developed by [Anatoly Frolov](https://www.ncbi.nlm.nih.gov/pubmed/?term=Frolov%20AS%5BAuthor%5D&cauthor=true&cauthor_uid=17690421) 
to run on personal digital assistants (PDAs).

## Features
EpiSample provides mainly with the following features:

### F1: Census
- Allows an enumerator to easily select a specific geographic region from a list
of hundreds or even thousands of place names by traversing through a
hierarchical list that starts from the highest geographic level (for example, Province) to the lowest level (for example, "EA" or Enumeration Area).
- Efficiently capture GPS coordinates of points (such as households), the name
of the head of the household and a comment.
- Send out multiple teams with multiple devices to conduct a census and
aggregate all of the census data onto one device (such as a team leader device)
for sampling, without the need for a laptop, Internet or cables.
- Capture points of interest to aid navigation and exclude these points from the sampling frame.

### F2: Sampling
- Apply a random sampling algorithm to a listing of points to select:
  - **Main points:** These are the primary points (typically households) to
  survey.
  - **Additional Points:** These are the oversampled points (if needed).
  - **Alternate Points:** These are the extra points which can be used in the
  event a household from the main points or additional points list is
  unavailable or *unsurveyable*.
- Lock-down the number of points to sample via password protected configuration,
or let the team lead choose how many to sample while in the field.
- Record the date/time of when the sample happened and the total number of
points included in the sampling frame.

### F3: Surveying
- Survey sampled points that were previously captured from the Collect module.
- Survey points that were imported from a CSV file.
- Filter and/or sort points in various combinations based on the type of point, distance from the device, time of data entry, interviewer name, Android device identification number, or enumeration area where the point is located.
- Navigate to a household using a built-in compass that shows the current
heading, the bearing (direction) to the household and the distance to the
household from the current position.
-Once at the household, bring up a survey and conduct it directly in ODK Survey.
