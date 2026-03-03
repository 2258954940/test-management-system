@echo off
chcp 65001 > nul  :: 解决中文乱码
set "output_file=项目代码汇总.txt"

:: 删除旧的导出文件
if exist "%output_file%" del "%output_file%"

echo 开始导出项目代码（排除node_modules/target/.git）... >> "%output_file%"
echo ===================== 项目目录：%cd% ===================== >> "%output_file%"
echo. >> "%output_file%"

:: 遍历所有代码文件，排除指定目录
for /r "%cd%" %%i in (*.vue,*.java,*.js,*.html,*.css,*.json,*.yml,*.properties,*.xml,*.md) do (
    :: 检查文件路径是否包含要排除的目录
    echo "%%i" | findstr /i /c:"node_modules" /c:"target" /c:".git" > nul
    :: 如果找到排除目录，就跳过这个文件（用goto实现跳过）
    if %errorlevel% equ 0 goto skip_file
    
    :: 导出有效代码文件
    echo ========== 文件路径：%%i ========== >> "%output_file%"
    echo. >> "%output_file%"
    type "%%i" >> "%output_file%"
    echo. >> "%output_file%"
    echo ============================================== >> "%output_file%"
    echo. >> "%output_file%"

:skip_file
)

echo 导出完成！文件保存在：%cd%\%output_file%
echo 请按任意键关闭窗口...
pause > nul