# WinSCP555
Plugin for Micro Focus Deployment Automation
這是為了使用Deployment Automation部署的一個簡單方案，目前仍採用WinSCP這個第三方元件傳輸，提供FTPS/SFTP兩種方式傳輸目錄檔案到指定位置，是一種簡易的檔案部署方式。

如何使用：
- [ ] 下載後，執行dev_resource\wrap_zip.sh
- [ ] 將產出的ZIP檔案透過DA選單[Administration]\[Automation]\[Load Plugin]上傳
- [ ] 傳輸成功後，plugin就會更新。隨著使用會傳輸到Agent去執行。

<a href="https://scan.coverity.com/projects/august-icekimo-winscp555">
  <img alt="Coverity Scan Build Status"
       src="https://scan.coverity.com/projects/29327/badge.svg"/>
</a>
