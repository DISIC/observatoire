# Oldcore: patches for [XWIKI-15307](https://jira.xwiki.org/browse/XWIKI-15307), [XWIKI-17141](https://jira.xwiki.org/browse/XWIKI-17141) and [XWIKI-17423
](https://jira.xwiki.org/browse/XWIKI-17423)

* xwiki-platform-legacy-oldcore-DINSICDEMA9.11.3-5ac27ae16a240b3e43376195312e34076dfe9ae4.jar (build from the branch https://github.com/xwikisas/xwiki-platform/tree/DINSICDEMA9.11.3 at head 5ac27ae16a240b3e43376195312e34076dfe9ae4) - build the oldcore first, skip the tests.

# Custom Solr API

* The custom xwiki-platform-search-solr-api jar is created for making it possible to index DBList property values – see https://github.com/DISIC/wikidemarches/issues/174
* It is built from the code located in the branch DINSICDEMA9.11.3 of xwikisas:xwiki-platform at:
  https://github.com/xwikisas/xwiki-platform/tree/DINSICDEMA9.11.3

# Dashboard macro

* The jar xwiki-platform-dashboard-macro-9.11.8-XWIKI-14247-1321d264e4cc2da38c395ad46d3ff2cc388d0dbb.jar is created from a checkout of the 9.11.8 tag of xwiki-platform (see above), cherry pick commit 86db078bf1ae3eb49ba5308f55bf3f9a3864e916 with modifications (so that it compiles) and then build module xwiki-platform-core/xwiki-platform-dashboard/xwiki-platform-dashboard-macro without tests (with -Dmaven.test.skip=true). This module could be used at version 9.11.8 because there are no changes between 9.11.3 and 9.11.8 in this module. The jar needs to replace xwiki-platform-dashboard-macro-9.11.3.jar .
