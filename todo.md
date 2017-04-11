# To Do List
- documentation
    - [ ] update java doc
    - [ ] add examples
- model handling
    - [ ] have list of preprocessing utilities managed by NeuroBNet
- model training
    - more generic handling of data preprocessing if possible
        - [ ] add more preprocessing tools
            - [ ] PCA: whitening
            - [ ] feature reduction
    - [ ] enhance training evaluation
        - [ ] distinction between classification and regression
        - [x] log confusion matrix
    - [ ] early stopping
- training set generation
    - [ ] shuffle data files
    - [ ] generation pipeline; flushing samples directly to files instead of 
        holding everything in memory first
    - [ ] data augmentation for larger training sets
    - [ ] dimensionality reduction methods
        - [ ] via RBM
        - [ ] via PCA
- training set analysis
    - [ ] t-SNE
    - [ ] PCA

# Future Work
- model check machines
    - generate 100 states per solver
        - give credit to fastest one
    - or: state space exploration over a fixed time
        - give credit to the one with the most states generated
