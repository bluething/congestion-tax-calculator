# LEARNING NOTES

## Key Issues Found

* Logic Bugs in `GetTollFee` method  
  * Line with `hour >= 8 && hour <= 14 && minute >= 30` should be `hour >= 8 && hour <= 14` (the minute condition is wrong)  
  * The 15:30-16:59 condition has incorrect logic: `hour == 15 && minute >= 0 || hour == 16 && minute <= 59` should use proper parentheses  
  * Missing handling for the 08:30-14:59 time slot properly  
* Deprecated Date API Usage:  
  * Using deprecated `Date.getHours()`, `Date.getMinutes()`, etc.  
  * The `IsTollFreeDate` method uses deprecated Date methods and incorrect day calculations  
* Vehicle Type Mismatch:  
  * `Motorbike` class returns "Motorbike" but the toll-free map expects "Motorcycle"  
* Missing 60-Minute Rule Logic Issues:  
  * The interval logic in `getTax` doesn't properly reset the interval start  
  * The maximum fee logic might not work correctly with multiple intervals  
* Architecture Issues:  
  * No entry point (as mentioned)  
  * Hardcoded tax rules  
  * No proper error handling  
  * Missing validation