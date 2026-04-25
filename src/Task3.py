# Task 3 Machine Learing
# Owen Redmond

import pandas as pd
import numpy as np

import sklearn as sk

from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
from sklearn.metrics import classification_report
from sklearn.metrics import confusion_matrix

df = pd.read_csv("Sample.csv") # Import data as a dataframe

df.replace(np.inf, np.nan, inplace=True) # Replace infs with NaNs
df.fillna(0, inplace=True) # Replace NaNs with 0s

df = df.drop(columns=["SchedType", "CoreCount", "CoreId", "IsCoreIdle"]) # Drop these 4 columns

# df prints
# pd.set_option('display.max_columns', None)
# print(df.head(10))

y = df['Action']
x = df.drop(columns=['Action'])

rowCount = len(df) # Create a variable for row count
col = x.columns # Create a variable for the 'list' of columns
columnCount = len(col) # Create a variable for column count


noneCount = y.value_counts() # Create a variable for action counts

print(f"Number of rows: {rowCount}\n") # Print number of rows

print("Action counts overall:")
print(f"{noneCount} \n") # Print the action counts

print(f"Number of features: {columnCount}\n")
print(f"Feature Names: {col}")


# ---------------------------


X_train, X_test, y_train, y_test = train_test_split(x, y, train_size=0.8, random_state=42)

model = RandomForestClassifier(n_estimators=1000, random_state=42, n_jobs=-1) # Create the model with the given values
model.fit(X_train, y_train)

y_pred = model.predict(X_test) # Prediction

accuracy = accuracy_score(y_test, y_pred, normalize=True) # Create the accuracy score
print(accuracy) # Print the score

target_names = ["NEXT_FCFS", "NEXT_OTHER", "NEXT_RR", "NEXT_SJF", "NONE"] # Create the target names for the classification report

classificaiton = classification_report(y_test, y_pred, target_names=target_names) # Create the classification report
print(classificaiton) # Print the report

cmatrix = confusion_matrix(y_test, y_pred) # Create the confusion matrix
print(cmatrix) # Print the matrix


# ---------------------------


print("\n" + "=" * 70)
print("SIMPLE SIMULATION (ML predicts next queue-level action)")
print("=" * 70)


# SIMULATION

processBool = False # Bool to loop until a proper input for processes

while processBool == False:
    processesInput = input("How many processes? (min 2): ") # Get num of processes
    if processesInput.isdigit(): # Check if input is a number
        numProcesses = int(processesInput) # Convert string to int
        if numProcesses <= 1: # If input is less than 2
            print("Minimum 2 processes required.") # Error message
        else:
            processBool = True # Exit loop
    else:
        print("Enter a numeric value.") # Error message for non int value


rrBool = False # Bool to loop until a proper input for rr time

while rrBool == False:
    rrInput = input("RR quantum (e.g., 2): ")
    if rrInput.isdigit(): # Check if input is a number
        rrTime = int(rrInput) # Convert string to int
        rrBool = True
    else:
        print("Enter a numeric value.") # Error message for non int value


processes = [] # Create processes list
count = 0 # Create count for processes


# For loop for each process inputs
for i in range(numProcesses):

    arrivalBool = False # Bool to loop until a proper input for arrival time
    while arrivalBool == False:
        arrival = input(f"P{count} arrival time: ")
        if arrival.replace('.', '', 1).isdigit(): # Check if input is a number
            arrival = float(arrival) # Convert string to float
            arrivalBool = True # Exit loop
        else:
            print("Enter a numeric value.") # Error message for non int value

    burstBool = False # Bool to loop until a proper input for burst time
    while burstBool == False:
        burst = input(f"P{count} burst time: ")
        if burst.replace('.', '', 1).isdigit(): # Check if input is a number
            burst = float(burst) # Convert string to float
            burstBool = True # Exit loop
        else:
            print("Enter a numeric value.") # Error message for non int value

    process = {"PID": f"P{count}", "Arrival": arrival, "Burst": burst, "Remaining": burst}
    processes.append(process)
    count += 1


# Simulation Setup

totalBursts    = sum(p["Burst"] for p in processes) # Total burst time across all processes
burstsExecuted = 0 # Total ticks executed so far

currentTime    = 0
readyQueue     = [] # Processes currently ready to run
currentProcess = None # Process currently on the CPU
rrTicksUsed    = 0 # Consecutive ticks the current process has used under RR
completed      = [] # PIDs that have finished

logRows        = [] # Collect one row per time step for the decision log

simulation = True


#  Main Simulation Loop

while simulation == True:

    # Add processes that arrive at current time to ready queue
    for p in processes:
        if p["Arrival"] == currentTime and p["PID"] not in completed and p not in readyQueue and p is not currentProcess:
            readyQueue.append(p)

    # Remove any completed processes from ready queue
    readyQueue = [p for p in readyQueue if p["Remaining"] > 0 and p["PID"] not in completed]

    queuePIDs = [p["PID"] for p in readyQueue] # Snapshot of ready queue PIDs for logging

    # Build remaining times list for feature calculation
    if readyQueue:
        remainingTimes = [p["Remaining"] for p in readyQueue]
    else:
        remainingTimes = [0] # Default to avoid errors when queue is empty

    # Build feature row to pass to the model
    features = {
        "QueueId":                    0.0,
        "QueueThreadCount":           float(len(readyQueue)),
        "QueueMaxRemainingBursts":    float(max(remainingTimes)),
        "QueueMinRemainingBursts":    float(min(remainingTimes)),
        "QueueMeanRemainingBursts":   float(np.mean(remainingTimes)),
        "QueueMedianRemainingBursts": float(np.median(remainingTimes)),
        "QueueRangeRemainingBursts":  float(max(remainingTimes) - min(remainingTimes)),
        "QueueTotalRemainingBursts":  float(sum(remainingTimes)),
        "CoreBurstAge":               0.0,
        "ThreadBurstsRan":            float(burstsExecuted),
        "ThreadBurstsRemaining":      float(totalBursts - burstsExecuted),
        "AvgThreadThroughput":        0.0,
        "AvgThreadTurnaround":        0.0,
    }

    featureDf = pd.DataFrame([features])[x.columns] # Match training column order

    predictedAction = model.predict(featureDf)[0] # Predict action using trained model

    # Override: NEXT_OTHER is not a valid action, default to NEXT_FCFS
    if predictedAction == "NEXT_OTHER":
        predictedAction = "NEXT_FCFS"

    # Override: NONE with nothing running means nothing to keep, default to NEXT_FCFS
    if predictedAction == "NONE" and currentProcess is None:
        predictedAction = "NEXT_FCFS"


    # Select next process based on predicted action

    if predictedAction == "NEXT_FCFS":
        if readyQueue:
            candidate = min(readyQueue, key=lambda p: p["Arrival"]) # Earliest arrival
            if candidate is not currentProcess:
                if currentProcess is not None and currentProcess["Remaining"] > 0 and currentProcess["PID"] not in completed:
                    if currentProcess not in readyQueue:
                        readyQueue.append(currentProcess) # Return preempted process to queue
                currentProcess = candidate
                rrTicksUsed = 0

    elif predictedAction == "NEXT_SJF":
        if readyQueue:
            candidate = min(readyQueue, key=lambda p: p["Remaining"]) # Shortest remaining burst
            if candidate is not currentProcess:
                if currentProcess is not None and currentProcess["Remaining"] > 0 and currentProcess["PID"] not in completed:
                    if currentProcess not in readyQueue:
                        readyQueue.append(currentProcess) # Return preempted process to queue
                currentProcess = candidate
                rrTicksUsed = 0

    elif predictedAction == "NEXT_RR":
        if readyQueue:
            if rrTicksUsed >= rrTime or currentProcess is None or currentProcess not in readyQueue: # Rotate if quantum expired
                if currentProcess is not None and currentProcess["Remaining"] > 0 and currentProcess["PID"] not in completed:
                    if currentProcess not in readyQueue:
                        readyQueue.append(currentProcess) # Return preempted process to back of queue
                currentProcess = readyQueue[0] # Pick next in queue
                rrTicksUsed = 0

    elif predictedAction == "NONE":
        if currentProcess is None or currentProcess["Remaining"] <= 0: # Fallback if nothing is running
            if readyQueue:
                currentProcess = min(readyQueue, key=lambda p: p["Arrival"])
                rrTicksUsed = 0


    # Handle IDLE: no process running and queue is empty
    if currentProcess is None or currentProcess["Remaining"] <= 0:
        if not readyQueue:
            remainingProcesses = [p for p in processes if p["PID"] not in completed]
            if not remainingProcesses: # All processes finished
                simulation = False
                break

            logRows.append({"Time": currentTime, "ReadyQueue": str(queuePIDs), "Action": predictedAction, "Chosen": "IDLE", "RemainingAfter": None})
            currentTime += 1
            continue
        else:
            currentProcess = min(readyQueue, key=lambda p: p["Arrival"]) # Fallback pick
            rrTicksUsed = 0


    chosenPID = currentProcess["PID"] # PID of process being run this tick

    if currentProcess in readyQueue:
        readyQueue.remove(currentProcess) # Remove from queue while it runs

    currentProcess["Remaining"] -= 1 # Execute one tick
    burstsExecuted += 1
    rrTicksUsed += 1

    remainingAfter = currentProcess["Remaining"] # Remaining burst after this tick

    logRows.append({"Time": currentTime, "ReadyQueue": str(queuePIDs), "Action": predictedAction, "Chosen": chosenPID, "RemainingAfter": remainingAfter})

    # Mark process as completed if it has finished
    if currentProcess["Remaining"] <= 0:
        completed.append(currentProcess["PID"])
        currentProcess = None
        rrTicksUsed = 0

    # End simulation when all processes are done
    if len(completed) == len(processes):
        simulation = False

    currentTime += 1


#  Decision Log Output

print("\n" + "=" * 70)
print("SCHEDULER DECISION LOG")
print("=" * 70)

logDf = pd.DataFrame(logRows, columns=["Time", "ReadyQueue", "Action", "Chosen", "RemainingAfter"]) # Create dataframe from log rows
print(logDf.to_string(index=True)) # Print full log