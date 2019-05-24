# Introduction
This repository stores my coursework for year 2 Distributed Network Architecture & Operating Systems module. This contains my solution to the provided problem.

# What is the Problem?
For this piece of coursework, we had to create a load balancer system in which nodes could register with a central load balancer who would distribute jobs to the registered nodes via a scheduling algorithm (weighted or non-weighted round-robin). The jobs would be sent in from an 'initiator' who, for the purposes of the coursework, just sent a time for which a node would sleep while it 'computed' the problem.

For this we were to use Java, and within Java specfically to use DatagramSocket and DatagramPacket in order to create the network connections neccessary.

# My Solution
My solution is comprised of three Java solutions:
 - Initiator - to send the jobs to the Load Balancer
 - Load Balancer - to distribute jobs to connected nodes
 - Nodes - to 'compute' the provided jobs

## Version Information
Java Version: `9.0.1`
IDE: `IntelliJ IDEA 2017.3.1 Build #IU-173.3942.27, December 11, 2017`

## Running
This assumes the use of IntelliJ.

 - Firstly, import the modules into IntelliJ
 - Run time arguments will need to be configured for each module.
	 - Initiator - Requires a client name, load balancer IP address, and load balancer port number
	 - Load Balancer - Requires the port for itself to use, and the schuedling algorithm to use, 'WEIGHTED' or 'NON_WEIGHTED'
	 - Node - Requries a node name, maximum capacity, node port to use, load balancer IP address, and load balancer port number
- The indivual modules can be ran in any order as both the initiator and node will try to reconnected at a specified time interval
- Jobs can be sent to the load balancer from the initiator via the menu interface in the command line
