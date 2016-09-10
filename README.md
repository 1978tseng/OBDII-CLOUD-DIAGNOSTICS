PRIMARY PURPOSE

The potential benefits for this sort of system are numerous. End users could simply plug a simple adapter into their vehicle and instead of having to taking their malfunctioning vehicle to a service shop, they could get a diagnosis or information about the problem right there in their driveway or garage. Another advantage is that a service shop can also perform vehicle diagnostics using this cloud based data.  Continuous vehicle monitoring and uploading to the cloud allows for other possibilities. A driver could be alerted to a possible pending issue within their vehicle even before an error indication light comes on. Continuous vehicle monitoring coupled with time stamped data logging allows for more in depth diagnostics to be done. Someone examining the time stamped logs could see all the OBD-II data about a vehicle dating back as long as there are logs, using this information to better diagnose a complex issue within the vehicle.

This project is important because it makes ideas like the ones mentioned above a reality, it is a step forward in creating a more wireless and automated world. It can make a common commuter vehicle another component of the ever growing internet of things (IoT), connecting the vehicle’s OBD-II systems to our smart phones, laptops, portable vehicle diagnostic tools, computers at home, or elsewhere. 

SYSTEM VALIDATION
Validation answers the question as to whether the system performs the right functions. For this project, the system is considered validated if a user cannot realize whether the OBD diagnostics is being performed on an actual vehicle or on the cloud based data of the same vehicle. Another aspect of the validation is to ensure that the system performs better (e.g., is able to diagnose more complex scenarios) than a typical diagnostics using a standard OBD scan tool.

System Validation was performed during the execution of the test cases. Detailed information about the results of the individual test cases can be found in Appendix C. To determine whether or not the test performed the right functions both of the outputs from the OBD Diagnostic Engine were examined. The two outputs, one representing data taken directly from the vehicle, and the other representing the data taken from the cloud would need to show similar vehicle states for each given test case. Furthermore, the log of error codes from the cloud data was examined to see if the data was in agreement with any changes in the vehicle’s other performance values. 
II.  CONCLUSIONS AND RECOMMENDATIONS

SUMMARY
Cloud Based OBD Diagnostics are indeed possible. This project has shown that vehicle data can be taken automatically at set intervals and that data remotely downloaded and analyzed with results similar to an analysis done directly on the vehicle. This was expected for most of the regular vehicle data and could almost be considered a trivial case, what is important is where this project takes a step beyond the usual analysis done on a vehicle. 

CONCLUSIONS
This project creates a historical log of the data collected from the vehicle and associates a timestamp with each response it gets from the vehicle. This provides a much more in depth look at a vehicle’s history and can provide insight into the status of a vehicle when the error occurred and what effects this error had on the vehicle’s performance while it was present.
This historical log is not something that is normally kept in a vehicle and is not something a mechanic would normally see when trying to access the OBD information of a vehicle to diagnose the fault.  It is also not something that is normally created by many of the current technologies that exist for monitoring a vehicle.  Some commercial tools allow for the logging of certain PIDs and values, but none of them have error logging where one can see and watch exactly when an error show up and when it moved from pending to stored. 

This feature can prove useful in everyday operation of a vehicle. Getting home to find a check engine light on in your vehicle and just knowing what kind of error or fault is present does little to help find what caused the fault to happen. An idea of the exact timeframe where the fault formed though can help a user narrow down where they were, what they were doing with their vehicle and what they might have done that could have caused the fault to arise.

The historical log extends beyond error codes though, and covers other common vehicle PID information. This data can also be requested and reviewed. Access to these historical logs can further assist the user of this system in diagnosing less cut and dry error codes such as PO420 which would indicate that the catalyst system efficiency is below threshold. This error code can be caused by a myriad of problems within the vehicle’s engine, and by having access to information about how the vehicle has been running it would be easier for someone with adequate training to understand exactly what the problem is.
Not only is the data collection of this project accurate, because of this log the depth of the data collected is also superior to other vehicle monitoring software.

RECOMMENDATIONS
The project while success, could be more robust. Finding a vehicle that could be tested on proved difficult, and more tests on varying models of vehicles could prove beneficial to further research in this area. The complexity of the faults created could be greater, with a mechanic available to assess whether or not the historical information provided by the OBD Diagnostic engine assisted in their diagnostic capability.

NOTE:
Most of the system is developed by extending and implementing the android bluetooth tutorial
https://developer.android.com/guide/topics/connectivity/bluetooth.html
