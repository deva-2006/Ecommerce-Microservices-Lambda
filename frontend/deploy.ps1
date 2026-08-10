Write-Host "Building frontend..."
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed."
    exit 1
}

Write-Host "Uploading to S3..."
aws s3 sync dist s3://my-ecommerce-frontend-deva2006 --delete

if ($LASTEXITCODE -ne 0) {
    Write-Host "S3 upload failed."
    exit 1
}

Write-Host "Invalidating CloudFront cache..."
aws cloudfront create-invalidation `
    --distribution-id E1UOOQSQUW4GDB `
    --paths "/*"

if ($LASTEXITCODE -ne 0) {
    Write-Host "CloudFront invalidation failed."
    exit 1
}

Write-Host ""
Write-Host "Deployment completed successfully!"