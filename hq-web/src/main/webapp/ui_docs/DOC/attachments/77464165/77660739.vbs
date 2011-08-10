Rem http://msdn.microsoft.com/library/default.asp?url=/library/en-us/wua_sdk/wua/using_wua_from_a_remote_computer.asp

Set updateSession  = CreateObject("Microsoft.Update.Session")
Set updateSearcher = updateSession.CreateupdateSearcher()

Set searchResult = _
    updateSearcher.Search("IsInstalled=0 and Type='Software'")

WScript.Echo searchResult.Updates.Count & " available updates..."

If searchResult.Updates.Count = 0 Then
    WScript.Quit(0)
End If

For I = 0 To searchResult.Updates.Count-1
    Set update = searchResult.Updates.Item(I)
    WScript.Echo I + 1 & ") " & update.Title
Next

WScript.Quit(1)
