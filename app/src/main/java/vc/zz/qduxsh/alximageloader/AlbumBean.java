package vc.zz.qduxsh.alximageloader;

/**
 * Created by Administrator on 2016/8/25.
 */
public class AlbumBean {
        /**
         * 文件夹的第一张图片路径 
         */
        private String topImagePath;
        /**
         * 文件夹名 
         */
        private String folderName;
        /**
         * 文件夹中的图片数 
         */
        private int imageCounts;

        public String getTopImagePath() {
            return topImagePath;
        }
        public void setTopImagePath(String topImagePath) {
            this.topImagePath = topImagePath;
        }
        public String getFolderName() {
            return folderName;
        }
        public void setFolderName(String folderName) {
            this.folderName = folderName;
        }
        public int getImageCounts() {
            return imageCounts;
        }
        public void setImageCounts(int imageCounts) {
            this.imageCounts = imageCounts;
        }

    @Override
    public String toString() {
        return "ImageBean{" +
                "folderName='" + folderName + '\'' +
                ", topImagePath='" + topImagePath + '\'' +
                ", imageCounts=" + imageCounts +
                '}';
    }
}
