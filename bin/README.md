# Oldcore: patches for [XWIKI-15307](https://jira.xwiki.org/browse/XWIKI-15307), [XWIKI-17141](https://jira.xwiki.org/browse/XWIKI-17141) and [XWIKI-17423](https://jira.xwiki.org/browse/XWIKI-17423)

* Clone XWiki platform: `git clone git@github.com:xwiki/xwiki-platform.git`
* Checkout tag 9.11.3: `git checkout xwiki-platform-9.11.3`
* Create a branch: DINSICDEMA-9.11.3
* In this branch, cherry-pick the following commits:
[6f8defa1fc77933baafa4368d907005a04094194](https://github.com/xwiki/xwiki-platform/commit/6f8defa1fc77933baafa4368d907005a04094194), [8741e2afddfda5632303c3969a0a03e2c9b3459e](https://github.com/xwiki/xwiki-platform/commit/8741e2afddfda5632303c3969a0a03e2c9b3459e), [eee5d157c5ec5d30b307039541e65abc9f7e8850](https://github.com/xwiki/xwiki-platform/commit/eee5d157c5ec5d30b307039541e65abc9f7e8850), [c90fcefaf7c749349222a1e9267ecb3e5dd44255](https://github.com/xwiki/xwiki-platform/commit/c90fcefaf7c749349222a1e9267ecb3e5dd44255),
[1bf4eadddf3ca35ba536f7add139d5027e1d4272](https://github.com/xwiki/xwiki-platform/commit/1bf4eadddf3ca35ba536f7add139d5027e1d4272)
* Build xwiki-platform-oldcore: `mvn clean install -Dmaven.test.skip=true`
* Build xwiki-platform-legacy/xwiki-platform-legacy-oldcore/
* You will get `xwiki-platform-legacy-oldcore-9.11.3.jar` ready to be renamed into `xwiki-platform-legacy-oldcore-DINSICDEMA9.11.3-5ac27ae16a240b3e43376195312e34076dfe9ae4.jar` to reflect the corresponding branch and commit, and ready to be deployed in the WEB-INF/lib directory in lieu of `xwiki-platform-legacy-oldcore-9.11.3.jar`
* A branch is maintained privately with all these changes at https://github.com/xwikisas/xwiki-platform/tree/DINSICDEMA9.11.3 and file `xwiki-platform-legacy-oldcore-DINSICDEMA9.11.3-5ac27ae16a240b3e43376195312e34076dfe9ae4.jar` was obtained from the build of that branch (as explained above) with head at 5ac27ae16a240b3e43376195312e34076dfe9ae4 .

# Custom Solr API

* The custom xwiki-platform-search-solr-api jar is created for making it possible to index DBList property values – see https://github.com/DISIC/wikidemarches/issues/174
* It is built from the code located in the branch DINSICDEMA9.11.3 of xwikisas:xwiki-platform at:
  https://github.com/xwikisas/xwiki-platform/tree/DINSICDEMA9.11.3
* A branch is maintained privately with all these changes at https://github.com/xwikisas/xwiki-platform/tree/DINSICDEMA9.11.3 and file `xwiki-platform-search-solr-api-DINSICDEMA9.11.3-45ad9b0e084d555ea5dc1361eb7edb41d61e30d0.jar` is obtained from the build of that branch (as explained above) with head at 45ad9b0e084d555ea5dc1361eb7edb41d61e30d0 .

# Dashboard macro

* The jar xwiki-platform-dashboard-macro-9.11.8-XWIKI-14247-1321d264e4cc2da38c395ad46d3ff2cc388d0dbb.jar is created from a checkout of the 9.11.8 tag of xwiki-platform (see above), cherry pick commit 86db078bf1ae3eb49ba5308f55bf3f9a3864e916 with modifications (so that it compiles) and then build module xwiki-platform-core/xwiki-platform-dashboard/xwiki-platform-dashboard-macro without tests (with -Dmaven.test.skip=true). This module could be used at version 9.11.8 because there are no changes between 9.11.3 and 9.11.8 in this module. The jar needs to replace xwiki-platform-dashboard-macro-9.11.3.jar .
* A branch is maintained privately with all these changes at https://github.com/xwikisas/xwiki-platform/commits/9.11.8-clients and file `xwiki-platform-dashboard-macro-9.11.8-XWIKI-14247-1321d264e4cc2da38c395ad46d3ff2cc388d0dbb.jar` is obtained from the build of that branch (as explained above) with head at 1321d264e4cc2da38c395ad46d3ff2cc388d0dbb .
