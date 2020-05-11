# How to build xwiki-platform-legacy-oldcore to fix [XWIKI-15307](https://jira.xwiki.org/browse/XWIKI-15307):

* Clone XWiki platform: `git clone git@github.com:xwiki/xwiki-platform.git`
* Checkout tag 9.11.3: `git checkout xwiki-platform-9.11.3`
* Create a branch: DINSICDEMA-9.11.3
* In this branch, cherry-pick the following commit: [6f8defa1fc77933baafa4368d907005a04094194](https://github.com/xwiki/xwiki-platform/commit/6f8defa1fc77933baafa4368d907005a04094194)
* Build xwiki-platform-oldcore: `mvn clean install -DskipTests=true`
* Build xwiki-platform-legacy/xwiki-platform-legacy-oldcore/
* You will get `xwiki-platform-legacy-oldcore-9.11.3.jar` ready to be renamed into `xwiki-platform-legacy-oldcore-DINSICDEMA9.11.3-b55fe5c5c2fb453bb4154999f94de7e014b7fa58.jar` to reflect the corresponding branch and commit, and ready to be deployed in the WEB-INF/lib directory in lieu of `xwiki-platform-legacy-oldcore-9.11.3.jar`

# Custom Solr API

* The custom xwiki-platform-search-solr-api jar is created for making it possible to index DBList property values – see https://github.com/DISIC/wikidemarches/issues/174
* It is built from the code located in the branch DINSICDEMA9.11.3 of xwikisas:xwiki-platform at:
  https://github.com/xwikisas/xwiki-platform/tree/DINSICDEMA9.11.3

# Dashboard macro

* The jar xwiki-platform-dashboard-macro-9.11.8-XWIKI-14247-1321d264e4cc2da38c395ad46d3ff2cc388d0dbb.jar is created from a checkout of the 9.11.8 tag of xwiki-platform (see above), cherry pick commit 86db078bf1ae3eb49ba5308f55bf3f9a3864e916 with modifications (so that it compiles) and then build module xwiki-platform-core/xwiki-platform-dashboard/xwiki-platform-dashboard-macro without tests (with -Dmaven.test.skip=true). This module could be used at version 9.11.8 because there are no changes between 9.11.3 and 9.11.8 in this module. The jar needs to replace xwiki-platform-dashboard-macro-9.11.3.jar .
