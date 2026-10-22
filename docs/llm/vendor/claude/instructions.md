this project will be hosted at https://github.com/kusl/hamza417-positional
original project was at 
https://github.com/Hamza417/Positional
this repository is public and available free of cost 
replace this readme file with a better version when delivering changes 
always make sure this readme is uptodate with the latest code changes where possible 
Use your best judgment
If only a few files (under five or there about) need to change, give me the full path of the files that need to change and the FULL file 
if there are a lot of files that need to change 
    Delivery
    Whole project as one tar.gz, unambiguous file placement
    Any files to delete on updates called out explicitly
No bespoke sed/patch scripts — full files only for anything that changes
Docs updated alongside code changes
Low verbosity, no code comments
I can't emphasize this 
NO CODE COMMENTS 
I have a small export.sh here with this repository 
that creates a dump.txt file 
which I will upload to the Claude project. 
Read this file every time 
and now you know why it is important to not add too much nonsense in the code 
the code should ideally fit within claude project without spilling over to "search mode" 
or worse without fitting in the project memory at all 
remove any code that is unused
give me a single git rm bla bla with all those files please
so please be concise 

I have added the following actions secrets
I hope the names are self explanatory
1. KEYSTORE_BASE64
2. STORE_PASSWORD
3. KEY_PASSWORD
4. KEY_ALIAS
these secrets are NOT the same as upstream 
please add a github actions yaml to 
1. build 
2. run all tests (add unit tests if they are not present / not complete)
3. publish a full release not a pre release with every single push to `master` branch

so bottom line, 
use best engineering practices, 
solid principles, 
latest versions of native android strategy such as kotlin compose 
and only use performant native code 
but support being installed older versions of android as much as possible 
try to not destroy the user's battery 
try to use engineering best practices 
solid principles 
always use latest versions of dependencies 
and latest versions of github actions 
this application will ONLY run on android 
do NOT do any cross platform nonsense like xamarin expo  or whatever 
also this app will NEVER be put on Google Play Store 
so we should support everything Android 12 and above 
why android 12 and above? 
so we can support automatic updates with obtainium and the likes 
if this assumption is incorrect, use your best judgment to support the lowest version of Android possible 
remember, we do not need to support google play store
