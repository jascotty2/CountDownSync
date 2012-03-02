Imports System.Runtime.InteropServices

Module Main

    '<DllImport("user32.dll", CharSet:=CharSet.Auto, ExactSpelling:=True)> Public Function GetActiveWindow() As IntPtr
    'End Function
    Public Declare Function GetForegroundWindow Lib "user32.dll" () As System.IntPtr

    Declare Function GetWindowThreadProcessId Lib "user32.dll" ( _
        ByVal hwnd As Int32, _
        ByRef lpdwProcessId As Int32) As Int32

    Sub Main()
        Dim ps As Process() = Process.GetProcessesByName("WorldOfTanks")

        If (ps.Length <> 1) Then
            Console.Out.WriteLine("false")
        Else
            Console.Out.WriteLine("true")
            Dim processID As Int32
            GetWindowThreadProcessId(GetForegroundWindow(), processID)
            Console.Out.WriteLine(processID = ps(0).Id)
            Console.Out.WriteLine(ps(0).MainModule.FileName)
            Console.Out.WriteLine(processID)
            Console.Out.WriteLine(ps(0).Id)
        End If
    End Sub

End Module
