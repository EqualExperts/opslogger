**Message Code:** GDS-000000

**Description:** The process worked, and data was inserted into the performance platform

**Recommended Action:** None. This message is for auditing purposes.

====================================

**Message Code:** GDS-000001

**Description:** Something completely unexpected happened.

**Recommended Action:** Take the stack trace provided with this message and raise a ticket with higher level support. This may ultimately require the attention of application developers.

====================================

**Message Code:** GDS-000002

**Description:** The application cannot connect to the performance platform. This message is not  normally caused by an HTTP error — it is logged when a TCP connection cannot be opened.

**Possible causes:**
- The url specified in the configuration file is incorrect.
- Firewall/proxy settings of some kind are preventing access.
- The performance platform is experiencing an outage.

**Recommended Action:** Check that the settings in the configuration file are correct. Check that proxy and firewall settings are correct. Diagnose connectivity issues  in coordination with the performance platform team to rule out an outage on their end. Once the connectivity issues are resolved, the application can be re-run with no side effects.

====================================

**Message Code:** GDS-000003

**Description:** An HTTP-level error occurred trying to make a test request to the performance platform. The specific HTTP response code will be included in the log message

**Possible causes:**
- Status code 404: The configuration file specifies the wrong url for the performance platform.
- Status code 403: The bearer token in the configuration file is probably incorrect.
- Status code 3XX: The url in the configuration file may be out of date.
- Status code 5XX: An error has occurred in the performance platform.

**Recommended Action:** Double check the settings in the configuration file, especially if this configuration has not been proven to work in this environment before. Ultimately, you may need to determine the correct configuration settings with the aid of the performance platform team. Once the issue is resolved, the application can be re-run with no side effects.

====================================

**Message Code:** GDS-000004

**Description:** The date range specified (or the default if a custom date range wasn’t specified) didn’t return any data at all (this is different from data that was all zeroes).

**Recommended Action:** Check ETL job settings, and possibly re-run ETL jobs for the affected dates. Once the problem is resolved, the application can be re-run with no side effects.

====================================

**Message Code:** GDS-000005

**Description:** The configuration file supplied is not valid. While the application cannot validate configuration file values, it validates that the keys match the expected keys, to protect against unexpected changes.

**Recommended Action:** Fix the configuration file. When this message is logged, the expected configuration file will be printed to standard error (although not generally redirected to the log file). You can re-run the application to see the expected configuration keys, and use this as a guide to fix any errors in the configuration file you’re attempting to use. Remember that if you are not using an HTTP proxy, you must leaves those values blank (including whitespace), not omit them entirely.

====================================

**Message Code:** GDS-000006

**Description:** The configuration file is not found. If a path is not specified on the command line, the default location for the configuration file is “configuration.properties” in the current directory (this is not necessarily the directory the jar file is in).

**Recommended Action:** Create a valid configuration file, and either put it in the right directory with the name configuration.properties or specify the correct path in the command line arguments.

====================================

**Message Code:** GDS-000007

**Description:** The application could not connect to the database. A stack trace and database message will be included with this message

**Recommended Action:** Identify the exact cause and fix the problem with assistance from an SLC database administrator.

====================================

**Message Code:** GDS-000008

**Description:** The application was able to successfully connect to the database and the performance platform — everything seems to be working as expected. If the application is is `-—dry-run` mode, this will be the last message emitted before the application exists successfully.

**Recommended Action:** None. This message is for audit/debugging purposes and indicates that everything is working as expected.