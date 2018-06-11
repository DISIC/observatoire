# How to build xwiki-platform-legacy-oldcore to fix [XWIKI-15307](https://jira.xwiki.org/browse/XWIKI-15307):

* Clone XWiki platform: `git clone git@github.com:xwiki/xwiki-platform.git`
* Checkout tag 9.11.3: `git checkout xwiki-platform-9.11.3`
* Create a branch: DINSICDEMA-9.11.3
* In this branch, cherry-pick the following commit: [6f8defa1fc77933baafa4368d907005a04094194](https://github.com/xwiki/xwiki-platform/commit/6f8defa1fc77933baafa4368d907005a04094194)
* Build xwiki-platform-oldcore: `mvn clean install -DskipTests=true`
* Build xwiki-platform-legacy/xwiki-platform-legacy-oldcore/
* You will get `xwiki-platform-legacy-oldcore-9.11.3.jar` ready to be renamed into `xwiki-platform-legacy-oldcore-DINSICDEMA9.11.3-b55fe5c5c2fb453bb4154999f94de7e014b7fa58.jar` to reflect the corresponding branch and commit, and ready to be deployed in the WEB-INF/lib directory in lieu of `xwiki-platform-legacy-oldcore-9.11.3.jar`
