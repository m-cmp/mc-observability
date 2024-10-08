#!/bin/bash
influx -execute "CREATE DATABASE insight"
influx -execute "CREATE DATABASE downsampling"