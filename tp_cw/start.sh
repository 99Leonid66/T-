#!/bin/bash

mvn compile
mvn exec:java -Dexec.mainClass="ru.mtuci.tp_cw.App"
